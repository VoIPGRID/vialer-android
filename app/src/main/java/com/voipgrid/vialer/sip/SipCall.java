package com.voipgrid.vialer.sip;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.voipgrid.vialer.firebase.FirebaseEventSubmitter;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.media.monitoring.CallMediaMonitor;
import com.voipgrid.vialer.media.monitoring.PacketStats;
import com.voipgrid.vialer.sip.SipConstants.CallMissedReason;
import com.voipgrid.vialer.statistics.CallCompletionStatsDispatcher;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.StringUtil;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.OnCallTsxStateParam;
import org.pjsip.pjsua2.StreamInfo;
import org.pjsip.pjsua2.TimeVal;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_flag;
import org.pjsip.pjsua2.pjsua_call_media_status;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;


/**
 * Call class used to interact with a call.
 */
public class SipCall extends org.pjsip.pjsua2.Call {

    @StringDef({CALL_DIRECTION_OUTGOING, CALL_DIRECTION_INCOMING})
    @Retention(RetentionPolicy.SOURCE)
    @interface CallDirection {}

    private Uri mPhoneNumberUri;

    private Logger mLogger;
    private SipBroadcaster mSipBroadcaster;
    private SipService mSipService;

    private boolean mCallIsConnected = false;
    private boolean mIsOnHold;
    private boolean mUserHangup = false;
    private boolean mCallDeclined = false;
    private boolean mCallCompletedElsewhere = false;
    private boolean mCallIsTransferred = false;
    private boolean mRingbackStarted = false;
    @CallDirection private String mCallDirection;
    private String mCallerId;
    private String mIdentifier;
    private String mPhoneNumber;
    private String mCurrentCallState = SipConstants.CALL_INVALID_STATE;
    private boolean mIpChangeInProgress = false;
    private String mMiddlewareKey;
    private String mMessageStartTime;
    private CallInfo mLastCallInfo;
    private CallMediaMonitor mCallMediaMonitor;
    private boolean isMicrophoneMuted = false;
    private Double mos;

    /**
     * An object that represents the original invite received.
     */
    private SipInvite invite;

    private static final String CALL_DIRECTION_OUTGOING = "outgoing";
    private static final String CALL_DIRECTION_INCOMING = "incoming";

    private static final int MICROPHONE_VOLUME_MUTED = 0;
    private static final int MICROPHONE_VOLUME_UNUTED = 2;

    @Override
    public void onCallTsxState(OnCallTsxStateParam prm) {
        super.onCallTsxState(prm);

        // Check if the call is an ringing incoming call.
        if (mCurrentCallState.equals(SipConstants.CALL_INCOMING_RINGING)) {
            // Early state. Is where a call is being cancelled or completed elsewhere.
            String packet = prm.getE().getBody().getTsxState().getSrc().getRdata().getWholeMsg();
            if (!packet.isEmpty()) {
                CallMissedReason reason = CallMissedReason.UNKNOWN;
                if (packet.contains(CallMissedReason.CALL_ORIGINATOR_CANCEL.toString())) {
                    reason = CallMissedReason.CALL_ORIGINATOR_CANCEL;
                    VialerStatistics.incomingCallWasCancelledByOriginator(this);
                } else if (packet.contains(CallMissedReason.CALL_COMPLETED_ELSEWHERE.toString())) {
                    reason = CallMissedReason.CALL_COMPLETED_ELSEWHERE;
                    VialerStatistics.incomingCallWasCompletedElsewhere(this);
                    mCallCompletedElsewhere = true;
                }

                if (reason != CallMissedReason.UNKNOWN) {
                    mSipBroadcaster.broadcastMissedCalls(reason);
                }
            }
        }
    }

    void setIsIPChangeInProgress(boolean inProgress){
        mIpChangeInProgress = inProgress;
    }

    /**
     * Constructor used for outbound calls.
     * @param sipService
     * @param sipAccount
     */
    public SipCall(SipService sipService, SipAccount sipAccount) {
        super(sipAccount);
        mSipService = sipService;
        mLogger = mSipService.getLogger();
        mSipBroadcaster = mSipService.getSipBroadcaster();
    }

    /**
     * Constructor used for incoming calls.
     * @param sipService
     * @param sipAccount
     * @param callId
     */
    public SipCall(SipService sipService, SipAccount sipAccount, int callId) {
        super(sipAccount, callId);
        mSipService = sipService;
        mLogger = mSipService.getLogger();
        mSipBroadcaster = mSipService.getSipBroadcaster();
    }

    public SipCall(SipService sipService, SipAccount sipAccount, int callId, SipInvite invite) {
        this(sipService, sipAccount, callId);
        this.invite = invite;
    }

    public int getCallDuration() {
        try {
            CallInfo callInfo = this.getInfo();
            return callInfo.getConnectDuration().getSec();
        } catch (Exception e) {
            return -1;
        }
    }

    public String getPrettyCallDuration() {
        return DateUtils.formatElapsedTime(getCallDuration());
    }

    /**
     * Get the call duration in milliseconds, this will use the stored callinfo
     * so should not be used for live duration reporting but only for getting the
     * duration at the end of the call.
     *
     * @return
     */
    public int getCallDurationInMilliseconds() {
        TimeVal timeVal = mLastCallInfo.getConnectDuration();

        return (timeVal.getSec() * 1000) + timeVal.getMsec();
    }

    public String getCodec() {
        try {
            if (!isConnected()) return "";

            StreamInfo streaminfo = this.getStreamInfo(0);
            return streaminfo.getCodecName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private float getBandwidthUsage() {
        float bandwidth = 0;
        try {
            bandwidth = SipCallStats.calculateBandwidthUsage(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bandwidth;
    }

    public String getIdentifier() {
        if (mIdentifier == null) {
            mIdentifier = UUID.randomUUID().toString();
        }
        return mIdentifier;
    }

    public void answer() throws Exception {
        CallOpParam callOpParam = new CallOpParam(true);
        callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_ACCEPTED);
        super.answer(callOpParam);
        mCallIsConnected = true;
    }

    public void decline() throws Exception {
        mCallDeclined = true;
        hangupWithStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
        VialerStatistics.userDeclinedIncomingCall(this);
    }

    public void hangup(boolean userHangup) throws Exception {
        mUserHangup = userHangup;
        hangupWithStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
        VialerStatistics.userDidHangUpCall(this);
    }

    public void toggleHold() throws Exception {
        CallOpParam callOpParam = new CallOpParam(true);
        if (!this.isOnHold()) {
            super.setHold(callOpParam);
            this.setIsOnHold(true);

            mCurrentCallState = SipConstants.CALL_PUT_ON_HOLD_ACTION;
            mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_PUT_ON_HOLD_ACTION);
        } else {
            CallSetting callSetting = callOpParam.getOpt();
            callSetting.setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
            super.reinvite(callOpParam);
            this.setIsOnHold(false);

            mCurrentCallState = SipConstants.CALL_UNHOLD_ACTION;
            mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_UNHOLD_ACTION);
        }
    }

    /**
     * Update the calls microphone volume.
     *
     * @param newVolume
     */
    private void updateMicrophoneVolume(long newVolume) {
        try {
            CallMediaInfoVector callMediaInfoVector = this.getInfo().getMedia();
            long size = callMediaInfoVector.size();
            for (int i = 0; i < size; i++) {
                CallMediaInfo callMediaInfo = callMediaInfoVector.get(i);
                if(callMediaInfo.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO) {
                    AudioMedia audioMedia = AudioMedia.typecastFromMedia(this.getMedia(i));
                    if (audioMedia != null) {
                        audioMedia.adjustRxLevel(newVolume);
                    }
                }
            }
        } catch (Exception e) {
            mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_UPDATE_MICROPHONE_VOLUME_FAILED);
            e.printStackTrace();
        }
    }

    /**
     * Toggle the microphone mute status of this call.
     *
     */
    public void toggleMute() {
        mLogger.d("Changing the mute status of this call from " + isMicrophoneMuted + " to " + !isMicrophoneMuted);
        updateMicrophoneVolume(isMicrophoneMuted ? MICROPHONE_VOLUME_UNUTED : MICROPHONE_VOLUME_MUTED);
        isMicrophoneMuted = !isMicrophoneMuted;
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), isMicrophoneMuted ? SipConstants.CALL_MUTED : SipConstants.CALL_UNMUTED);
    }

    /**
     * Attended transfer to a second existing call. TODO: Not implemented yet.
     * @param transferTo
     * @throws Exception
     */
    public void xFerReplaces(SipCall transferTo) throws Exception {
        mCallIsTransferred = true;
        transferTo.setCallIsTransferred(true);
        CallOpParam callOpParam = new CallOpParam(true);
        super.xferReplaces(transferTo, callOpParam);
    }

    /**
     * Function to perform a hangup with a certain status code to be able to distinguish between
     * a hangup and a decline.
     * @param statusCode
     * @throws Exception
     */
    private void hangupWithStatusCode(pjsip_status_code statusCode) throws Exception {
        CallOpParam callOpParam = new CallOpParam(true);
        callOpParam.setStatusCode(statusCode);
        super.hangup(callOpParam);
    }

    private void setIsOnHold(boolean onHold) {
        mIsOnHold = onHold;
    }

    public boolean isOnHold() {
        return mIsOnHold;
    }

    /**
     * Translate the callback to the interface, which is currently implemented by the SipService.
     *
     * @param onCallStateParam parameters containing the state of an active call.
     */
    @Override
    public void onCallState(OnCallStateParam onCallStateParam) {
        try {
            mLastCallInfo = getInfo();  // Check to see if we can get CallInfo with this callback.

            pjsip_inv_state callState = mLastCallInfo.getState();
            mLogger.i("CallState changed!");
            mLogger.i(callState.toString());

            if (callState == pjsip_inv_state.PJSIP_INV_STATE_CALLING) {
                onCallStartRingback();
            }  else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                // Call has been setup, stop ringback.
                onCallStopRingback();
                onCallConnected();
            } else if (callState == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                if (mIpChangeInProgress && mCurrentCallState.equals(SipConstants.CALL_INCOMING_RINGING)) {
                    mLogger.d("Network switch during ringing phase.");
                }

                boolean wasInCall = mCallIsConnected;
                onCallStopRingback();
                onCallDisconnected();
                if (mCallCompletedElsewhere) {
                    mSipService.getNotification().answeredElsewhere(mPhoneNumber);
                } else if (!wasInCall && !mUserHangup && !mCallDeclined && isIncoming() && !mCallIsTransferred) {
                    mSipService.getNotification().missed(mPhoneNumber, mCallerId);
                }
                delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
            this.onCallInvalidState(e);
        }
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @param onCallMediaStateParam parameters containing the state of the an active call' media.
     */
    @Override
    public void onCallMediaState(OnCallMediaStateParam onCallMediaStateParam) {
        // Find suitable audio stream by looping.
        try {
            CallInfo ci = getInfo();
            CallMediaInfoVector media = ci.getMedia();
            // Administration to see if we connected some media to this call.
            boolean mediaAvailable = false;
            for (int i = 0; i < media.size(); ++i) {
                CallMediaInfo cmi = media.get(i);
                boolean usableStatus = (cmi.getStatus() ==
                        pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                        cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD);
                if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO && usableStatus) {
                    Media m = getMedia(i);
                    AudioMedia audio = AudioMedia.typecastFromMedia(m);
                    mediaAvailable = true; // Do some administration to notify about present media
                    this.onCallMediaAvailable(audio);
                }
            }
            if (!mediaAvailable) {
                this.onCallMediaUnavailable();
            }
        } catch (Exception e) {
            this.onCallInvalidState(e);
        }
    }

    public String getCurrentCallState() {
        return mCurrentCallState;
    }

    public Boolean isConnected() {
        return mCallIsConnected;
    }

    /**
     * Check if the call is currently in a ringing state.
     *
     * @return TRUE if in a ringing state, otherwise false.
     */
    public boolean isCallRinging() {
        return getCurrentCallState().equals(SipConstants.CALL_INCOMING_RINGING);
    }

    public Uri getPhoneNumberUri() {
        return mPhoneNumberUri;
    }

    public void setPhoneNumberUri(Uri phoneNumberUri) {
        this.mPhoneNumberUri = phoneNumberUri;
    }

    public String getPhoneNumber() {
        return PhoneNumberUtils.maskAnonymousNumber(getAppropriateCallerInformationHeader().number);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.mPhoneNumber = phoneNumber;
    }

    public String getCallerId() {
        return getAppropriateCallerInformationHeader().name;
    }

    /**
     * This will select the correct header from the INVITE based on the priority that we should
     * be displaying the various caller information headers.
     *
     * @return
     */
    private SipInvite.CallerInformationHeader getAppropriateCallerInformationHeader() {
        SipInvite.CallerInformationHeader defaultCallerInformation = new SipInvite.CallerInformationHeader(mCallerId, mPhoneNumber);

        if (this.invite == null) {
            return defaultCallerInformation;
        }

        if (this.invite.hasPAssertedIdentity()) {
            return this.invite.getPAssertedIdentity();
        }

        if (this.invite.hasRemotePartyId()) {
            return this.invite.getRemotePartyId();
        }

        return defaultCallerInformation;
    }

    public void setCallerId(String callerId) {
        this.mCallerId = callerId;
    }

    public void onCallIncoming() {
        mLogger.d("onCallIncoming");
        mCallDirection = CALL_DIRECTION_INCOMING;

        if (!canHandleIncomingCall()) {
            answerWithCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
            return;
        }

        if (mSipService.getCurrentCall() != null) {
            VialerStatistics.incomingCallFailedDueToOngoingVialerCall(this);
        }

        if (mSipService.getNativeCallManager().isBusyWithNativeCall()) {
            VialerStatistics.incomingCallFailedDueToOngoingGsmCall(this);
        }

        mCurrentCallState = SipConstants.CALL_INCOMING_RINGING;
        mSipService.setCurrentCall(this);

        try {
            answerWithCode(pjsip_status_code.PJSIP_SC_RINGING);
        } catch (Exception e) {
            onCallInvalidState(e);
            return;
        }

        Intent incomingCallDetails = mSipService.getIncomingCallDetails();

        if (incomingCallDetails == null) {
            throw new RuntimeException("We have no incoming call details and are therefore unable to start this call");
        }

        setCallerId(incomingCallDetails.getStringExtra(SipConstants.EXTRA_CONTACT_NAME));
        setPhoneNumber(incomingCallDetails.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER));
        mSipService.informUserAboutIncomingCall(getPhoneNumber(), getCallerId());
    }

    /**
     * Answer the current call with a code, throwing a runtime exception if it fails.
     *
     * @param code
     * @return
     */
    private void answerWithCode(pjsip_status_code code) {
        CallOpParam callOpParam = new CallOpParam();
        callOpParam.setStatusCode(code);
        try {
            this.answer(callOpParam);
            mLogger.i("Answered call with code: " + code);
        } catch (Exception e) {
            mLogger.e("Failed to answer call with code " + code);
            throw new RuntimeException(e);
        }
    }

    /**
     * Check to see if we have any other calls in progress currently, if so we cannot handle an incoming call.
     *
     * @return
     */
    private boolean canHandleIncomingCall() {
        boolean result = mSipService.getCurrentCall() == null && !mSipService.getNativeCallManager().isBusyWithNativeCall();

        if (!result) LogHelper.using(mLogger).logBusyReason(mSipService);

        return result;
    }

    /**
     * Start an outgoing call.
     *
     * @param phoneNumber The phone number uri to call
     * @return TRUE if the call has been successfully started, otherwise FALSE.
     */
    boolean startOutgoingCall(Uri phoneNumber) {
        mLogger.d("onCallOutgoing");
        mCallDirection = CALL_DIRECTION_OUTGOING;

        CallOpParam callOpParam = new CallOpParam();
        callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
        try {
            super.makeCall(phoneNumber.toString(), callOpParam);

            mSipService.setCurrentCall(this);

            return true;
        } catch (Exception e) {
            onCallInvalidState(e);
            return false;
        }
    }

    private void onCallConnected() {
        mLogger.d("onCallConnected");
        mCallIsConnected = true;
        mCurrentCallState = SipConstants.CALL_CONNECTED_MESSAGE;
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_CONNECTED_MESSAGE);
        mCallMediaMonitor = new CallMediaMonitor(this);
        new Thread(mCallMediaMonitor).start();
        mSipService.changeNotification(mSipService.getNotification().active(this));
    }

    /**
     * Find the media packets sent/received for this call.
     *
     * @return
     */
    public @Nullable PacketStats getMediaPacketStats() {
        return PacketStats.Builder.fromSipCall(this);
    }

    public @Nullable PacketStats getLastMediaPacketStats() {
        if (mCallMediaMonitor == null) {
            return null;
        }

        return mCallMediaMonitor.getMostRecentPacketStats();
    }

    private void onCallDisconnected() {
        mLogger.d("onCallDisconnected");
        FirebaseEventSubmitter.INSTANCE.userCompletedCall(this);
        mCallIsConnected = false;
        mSipService.removeCallFromList(this);
        mCurrentCallState = SipConstants.CALL_DISCONNECTED_MESSAGE;
        mSipBroadcaster.broadcastCallStatus(
            getIdentifier(),
            SipConstants.CALL_DISCONNECTED_MESSAGE,
            mLastCallInfo.getLastStatusCode()
        );
        new CallCompletionStatsDispatcher().callDidComplete(this);

        // Play end of call beep only when the remote party hangs up and the call was connected.
        if (!mUserHangup && !mCallIsTransferred) {
            VialerStatistics.remoteDidHangUpCall(this);
            mSipService.playBusyTone();
        }
    }

    private void onCallInvalidState(Throwable fault) {
        mLogger.d("onCallInvalidState");
        mLogger.d("" + Log.getStackTraceString(fault));
        mSipService.removeCallFromList(this);
        mCurrentCallState = SipConstants.CALL_INVALID_STATE;
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_INVALID_STATE);
        fault.printStackTrace();
    }

    private void onCallMediaAvailable(AudioMedia media) {
        mLogger.d("onCallMediaAvailable");
        try {
            // There is media available so stop the ringback.
            onCallStopRingback();

            // Connect de audio device manager to the sip media.
            AudDevManager audDevManager = mSipService.getSipConfig().getEndpoint().audDevManager();
            media.startTransmit(audDevManager.getPlaybackDevMedia());
            audDevManager.getCaptureDevMedia().startTransmit(media);

            mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_MEDIA_AVAILABLE_MESSAGE);
        } catch (Exception e) {

            mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_MEDIA_FAILED);
            e.printStackTrace();
        }
    }

    private void onCallMediaUnavailable() {
        mLogger.d("onCallMediaUnavailable");
    }

    private void onCallStartRingback() {
        mLogger.d("onCallStartRingback: " + mRingbackStarted);
        if (!mRingbackStarted) {
            mLogger.d("Ringback not started. Start it.");
            mRingbackStarted = true;
            mSipService.startRingback();
        }
    }

    private void onCallStopRingback() {
        mLogger.d("onCallStopRingback");
        mSipService.stopRingback();
    }

    boolean getCallIsTransferred() {
        return mCallIsTransferred;
    }

    private void setCallIsTransferred(boolean transferred) {
        mCallIsTransferred = transferred;
    }

    private Object getConnectionType() {
        return new ConnectivityHelper(
                (ConnectivityManager) mSipService.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) mSipService.getSystemService(Context.TELEPHONY_SERVICE)
        ).getConnectionTypeString();
    }

    boolean isIpChangeInProgress() {
        return mIpChangeInProgress;
    }

    public void setMiddlewareKey(String middlewareKey) {
        mMiddlewareKey = middlewareKey;
    }

    public void setMessageStartTime(String messageStartTime) {
        mMessageStartTime = messageStartTime;
    }

    public String getMiddlewareKey() {
        return mMiddlewareKey;
    }

    public String getMessageStartTime() {
        return mMessageStartTime;
    }

    public String getAsteriskCallId() {
        CallInfo callInfo = getLastCallInfo();

        return callInfo != null ? callInfo.getCallIdString() : null;
    }

    public String getCallDirection() {
        return mCallDirection;
    }

    public boolean isOutgoing() {
        return CALL_DIRECTION_OUTGOING.equals(getCallDirection());
    }

    public boolean isIncoming() {
        return CALL_DIRECTION_INCOMING.equals(getCallDirection());
    }

    /**
     * Extracts the call transport (e.g. TLS/TCP/UDP) from the local contact string.
     *
     */
    public String getTransport() {
        try {
            CallInfo callInfo = getLastCallInfo();

            if (callInfo == null) {
                return null;
            }

            String transport = callInfo.getLocalContact();

            if (transport == null) {
                return null;
            }

            if (!transport.contains("transport")) {
                return null;
            }

            return StringUtil.extractFirstCaptureGroupFromString(transport, "transport=([^;]+);");
        } catch (Exception e) {
            mLogger.e("Unable to get call id: " + e.getMessage());
            return null;
        }
    }

    /**
     * Will return the last callinfo recovered during call state change, if no lastCallInfo exists, it will attempt to retrieve it.
     *
     * @return
     */
    private @Nullable CallInfo getLastCallInfo() {
        try {
            if (mLastCallInfo == null) {
                mLastCallInfo = getInfo();
            }

            return mLastCallInfo;
        } catch (Exception e) {
            mLogger.e("Unable to get call info");
            return null;
        }
    }

    public boolean hasCalculatedMos() {
        return mos != null;
    }

    public void setMos(double mos) {
        this.mos = mos;
    }

    public double getMos() {
        return mos;
    }

    public SipService getSipService() {
        return mSipService;
    }

    public boolean isMuted() {
        return isMicrophoneMuted;
    }
}

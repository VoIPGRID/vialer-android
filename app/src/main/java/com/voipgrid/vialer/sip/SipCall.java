package com.voipgrid.vialer.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.ConnectivityHelper;

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
import org.pjsip.pjsua2.RtcpStat;
import org.pjsip.pjsua2.RtcpStreamStat;
import org.pjsip.pjsua2.StreamInfo;
import org.pjsip.pjsua2.StreamStat;
import org.pjsip.pjsua2.TimeVal;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_flag;
import org.pjsip.pjsua2.pjsua_call_media_status;

import java.util.UUID;


/**
 * Call class used to interact with a call.
 */
public class SipCall extends org.pjsip.pjsua2.Call {
    public static final String TAG = SipCall.class.getSimpleName();

    private Uri mPhoneNumberUri;

    private RemoteLogger mRemoteLogger;
    private SipBroadcaster mSipBroadcaster;
    private SipService mSipService;

    private boolean mCallIsConnected = false;
    private boolean mIsOnHold;
    private boolean mOutgoingCall = false;
    private boolean mUserHangup = false;
    private boolean mCallIsTransferred = false;
    private boolean mRingbackStarted = false;
    private String mCallerId;
    private String mIdentifier;
    private String mPhoneNumber;
    private  String mCurrentCallState;

    private int mNetworkSwitchTime = 0;

    private BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleNetworkStateChange();
        }
    };

    private void startNetworkingListener() {
        mSipService.registerReceiver(
                mNetworkStateReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        );
    }

    private void stopNetworkingListener() {
        mSipService.unregisterReceiver(mNetworkStateReceiver);
    }

    private void handleNetworkStateChange() {
        if (mCallIsConnected) {
            sendMos();
            mNetworkSwitchTime = getCallDuration();
        }
    }

    private void sendMos() {
        if ((getCallDuration() - mNetworkSwitchTime) > 10) {
            new AnalyticsHelper(((AnalyticsApplication) mSipService.getApplication()).getDefaultTracker()).sendEvent(
                    mSipService.getString(R.string.analytics_event_category_metrics),
                    mSipService.getString(R.string.analytics_event_action_callmetrics),
                    mSipService.getString(R.string.analytics_event_label_mos, getCodec(), getConnectionType()),
                    (int) (100 * (long) this.calculateMos())
            );
        }
    }

    private void sendBandwidth() {
        if (getCallDuration() > 10) {
            new AnalyticsHelper(((AnalyticsApplication) mSipService.getApplication()).getDefaultTracker()).sendEvent(
                    mSipService.getString(R.string.analytics_event_category_metrics),
                    mSipService.getString(R.string.analytics_event_action_callmetrics),
                    mSipService.getString(R.string.analytics_event_label_bandwith, getCodec()),
                    (int) this.getBandwidthUsage() * 1024
            );
        }
    }

    /**
     * Constructor used for outbound calls.
     * @param sipService
     * @param sipAccount
     */
    public SipCall(SipService sipService, SipAccount sipAccount) {
        super(sipAccount);
        mSipService = sipService;
        mRemoteLogger = mSipService.getRemoteLogger();
        mSipBroadcaster = mSipService.getSipBroadcaster();
        startNetworkingListener();
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
        mRemoteLogger = mSipService.getRemoteLogger();
        mSipBroadcaster = mSipService.getSipBroadcaster();
        startNetworkingListener();
    }

    public int getCallDuration() {
        TimeVal timeVal = new TimeVal();
        try {
            CallInfo callInfo = this.getInfo();
            timeVal = callInfo.getConnectDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return timeVal.getSec();
    }

    private String getCodec() {
        try {
            StreamInfo mStreaminfo = this.getStreamInfo(0);
            return mStreaminfo.getCodecName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private long getBandwidthUsage() {
        long bandwidth = 0;
        try {
            bandwidth = this.getStreamStat(0).getRtcp().getRxStat().getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Divide to get MB's
        return 1024 * 1024 / bandwidth;
    }

    /**
     *  This will calculate the current MOS value of the call.
     *
     *  Credits to: https://www.pingman.com/kb/article/how-is-mos-calculated-in-pingplotter-pro-50.html
     */
    private float getR() {
        float r = 0;
        try {
            StreamStat mStreamStat = this.getStreamStat(0);

            RtcpStat rtcpStat = mStreamStat.getRtcp();
            RtcpStreamStat rtcpStreamStat = rtcpStat.getRxStat();

            float rxJitter = (float) rtcpStreamStat.getJitterUsec().getMean() / 1000;
            float averageRoundTripTime = (float) rtcpStat.getRttUsec().getMean() / 1000;
            if (averageRoundTripTime == 0) {
                averageRoundTripTime = 20; // Estimated by Bob
            }

            // Take the average latency, add jitter, but double the impact to latency
            // then add 10 for protocol latencies
            float effectiveLatency = averageRoundTripTime + rxJitter * 2 + 10;

            // Implement a basic curve - deduct 4 for the R value at 160ms of latency
            // (round trip).  Anything over that gets a much more aggressive deduction.
            if (effectiveLatency < 160) {
                r = 93.2f - (effectiveLatency / 40);
            } else {
                r = 93.2f - (effectiveLatency - 120) / 10;
            }

            // Number of packets send and received.
            float rxPackets = rtcpStreamStat.getPkt();
            // Percentage package loss (100 = 100% loss - 0 = 0% loss)
            float rxLoss = rtcpStreamStat.getLoss();
            float rxPacketLoss;
            if (rxPackets == 0) {
                rxPacketLoss = 100f;
            } else {
                rxPacketLoss = (rxLoss / (rxPackets + rxLoss)) * 100f;
            }

            // Now, let's deduct 2.5 R values per percentage of packet loss.
            r = r - (rxPacketLoss * 2.5f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    private float calculateMos() {
        float r = getR();
        if (r > 0) {
            return 1f + 0.035f * r + .000007f * r * (r - 60f) * (100f - r);
        }
        return 0f;
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
        hangupWithStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
    }

    public void hangup(boolean userHangup) throws Exception {
        mUserHangup = userHangup;
        hangupWithStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
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

    public void updateMicrophoneVolume(long newVolume) {
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
            CallInfo info = getInfo();  // Check to see if we can get CallInfo with this callback.
            pjsip_inv_state callState = info.getState();

            if (callState == pjsip_inv_state.PJSIP_INV_STATE_CALLING) {
                // We are handling a outgoing call.
                mOutgoingCall = true;
                onCallStartRingback();
            }  else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                // Call has been setup, stop ringback.
                onCallStopRingback();
                onCallConnected();
            } else if (callState == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                onCallStopRingback();
                onCallDisconnected();
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

    public Boolean getIsCallConnected() {
        return mCallIsConnected;
    }

    public Uri getPhoneNumberUri() {
        return mPhoneNumberUri;
    }

    public void setPhoneNumberUri(Uri phoneNumberUri) {
        this.mPhoneNumberUri = phoneNumberUri;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.mPhoneNumber = phoneNumber;
    }

    public String getCallerId() {
        return mCallerId;
    }

    public void setCallerId(String callerId) {
        this.mCallerId = callerId;
    }

    public void onCallIncoming() {
        mRemoteLogger.d(TAG + " onCallIncoming");

        // Determine whether we can accept the incoming call.
        pjsip_status_code code = pjsip_status_code.PJSIP_SC_RINGING;
        if (mSipService.getCurrentCall() != null || mSipService.nativeCallHasBeenAnswered() || mSipService.nativeCallIsRinging()) {
            code = pjsip_status_code.PJSIP_SC_BUSY_HERE;
        }

        mCurrentCallState = SipConstants.CALL_INCOMING_RINGING;

        // If we send back a ringing set the current call.
        if (code.equals(pjsip_status_code.PJSIP_SC_RINGING)) {
            mSipService.setCurrentCall(this);
        }
        try {
            CallOpParam callOpParam = new CallOpParam();
            callOpParam.setStatusCode(code);
            this.answer(callOpParam);

            // If we sent a ringing setup the CallActivity for a incoming call. This is always
            // the first call because we do not support incoming calls when there is a call
            // active.
            if (code.equals(pjsip_status_code.PJSIP_SC_RINGING)) {
                Intent incomingCallDetails = mSipService.getIncomingCallDetails();
                String callerId = "";
                String number = "";
                if (incomingCallDetails != null) {
                    callerId = incomingCallDetails.getStringExtra(SipConstants.EXTRA_CONTACT_NAME);
                    number = incomingCallDetails.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER);
                }

                mSipService.startIncomingCallActivity(number, callerId);
            }
        } catch (Exception e) {
            onCallInvalidState(e);
        }
    }

    public void onCallOutgoing(Uri phoneNumber, boolean startActivity) {
        mRemoteLogger.d(TAG + " onCallOutgoing");
        CallOpParam callOpParam = new CallOpParam();
        callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
        try {
            super.makeCall(phoneNumber.toString(), callOpParam);

            mSipService.setCurrentCall(this);

            if (startActivity) {
                mSipService.startOutgoingCallActivity(this, getPhoneNumberUri());
            }
        } catch (Exception e) {
            onCallInvalidState(e);
        }
    }

    public void onCallConnected() {
        mRemoteLogger.d(TAG + " onCallConnected");
        mCallIsConnected = true;
        mCurrentCallState = SipConstants.CALL_CONNECTED_MESSAGE;
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_CONNECTED_MESSAGE);
    }

    public void onCallDisconnected() {
        mRemoteLogger.d(TAG + " onCallDisconnected");
        sendMos();
        sendBandwidth();
        stopNetworkingListener();

        // Play end of call beep only when the remote party hangs up and the call was connected.
        if (!mUserHangup && mCallIsConnected && !mCallIsTransferred) {
            mSipService.playBusyTone();
        }
        mCallIsConnected = false;
        // Remove this call from the service.
        mSipService.removeCallFromList(this);
        mCurrentCallState = SipConstants.CALL_DISCONNECTED_MESSAGE;
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_DISCONNECTED_MESSAGE);
    }

    public void onCallInvalidState(Throwable fault) {
        mRemoteLogger.d(TAG + " onCallInvalidState");
        mRemoteLogger.d(TAG + " " + Log.getStackTraceString(fault));
        mSipService.removeCallFromList(this);
        mCurrentCallState = SipConstants.CALL_INVALID_STATE;
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_INVALID_STATE);
        fault.printStackTrace();
    }

    public void onCallMediaAvailable(AudioMedia media) {
        mRemoteLogger.d(TAG + " onCallMediaAvailable");
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

    public void onCallMediaUnavailable() {
        mRemoteLogger.d(TAG + " onCallMediaUnavailable");
    }

    public void onCallStartRingback() {
        if (!mRingbackStarted) {
            mRemoteLogger.d(TAG + " onCallStartRingback");
            mSipService.startRingback();
            mRingbackStarted = true;
        }
    }

    public void onCallStopRingback() {
        if (mRingbackStarted) {
            mRemoteLogger.d(TAG + " onCallStopRingback");
            mSipService.stopRingback();
            mRingbackStarted = false;
        }
    }

    public boolean getCallIsTransferred() {
        return mCallIsTransferred;
    }

    public void setCallIsTransferred(boolean transferred) {
        mCallIsTransferred = transferred;
    }

    public Object getConnectionType() {
        return new ConnectivityHelper(
                (ConnectivityManager) mSipService.getSystemService(mSipService.CONNECTIVITY_SERVICE),
                (TelephonyManager) mSipService.getSystemService(mSipService.TELEPHONY_SERVICE)
        ).getConnectionTypeString();
    }
}

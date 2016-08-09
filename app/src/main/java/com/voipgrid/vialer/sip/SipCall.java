package com.voipgrid.vialer.sip;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.voipgrid.vialer.logging.RemoteLogger;

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
    private String mCallerId;
    private String mIdentifier;
    private String mPhoneNumber;

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
            mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_PUT_ON_HOLD_ACTION);
        } else {
            CallSetting callSetting = callOpParam.getOpt();
            callSetting.setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
            super.reinvite(callOpParam);
            this.setIsOnHold(false);
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

    public void setIsOnHold(boolean onHold) {
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
            } else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONNECTING
                    || callState == pjsip_inv_state.PJSIP_INV_STATE_EARLY) {

                // Start ringing in early state on outgoing call.
                if (callState == pjsip_inv_state.PJSIP_INV_STATE_EARLY) {
                    if (!hasMedia() && mOutgoingCall) {
                        onCallStartRingback();
                    }
                }

                pjsip_status_code lastStatusCode = info.getLastStatusCode();
                if (hasMedia() &&
                        (lastStatusCode == pjsip_status_code.PJSIP_SC_PROGRESS ||
                                lastStatusCode == pjsip_status_code.PJSIP_SC_OK)) {
                    onCallStopRingback();  // if so stop the ringback
                }
            } else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
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
            for(int i=0; i < media.size(); ++i) {
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
        pjsip_status_code code = mSipService.getCurrentCall() != null ? pjsip_status_code.PJSIP_SC_BUSY_HERE : pjsip_status_code.PJSIP_SC_RINGING;

        CallOpParam callOpParam = new CallOpParam();
        callOpParam.setStatusCode(code);

        // If we send back a ringing set the current call.
        if (code.equals(pjsip_status_code.PJSIP_SC_RINGING)) {
            mSipService.setCurrentCall(this);
        }
        try {
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
        mSipService.startGsmCallListener();
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_CONNECTED_MESSAGE);
    }

    public void onCallDisconnected() {
        mRemoteLogger.d(TAG + " onCallDisconnected");
        // Play end of call beep only when the remote party hangs up and the call was connected.
        if (!mUserHangup && mCallIsConnected) {
            mSipService.playBusyTone();
        }
        // Remove this call from the service.
        mSipService.removeCallFromList(this);
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_DISCONNECTED_MESSAGE);
    }

    public void onCallInvalidState(Throwable fault) {
        mRemoteLogger.d(TAG + " onCallInvalidState");
        mRemoteLogger.d(TAG + " " + Log.getStackTraceString(fault));
        mSipBroadcaster.broadcastCallStatus(getIdentifier(), SipConstants.CALL_INVALID_STATE);
        mSipService.removeCallFromList(this);
        fault.printStackTrace();
    }

    public void onCallMediaAvailable(AudioMedia media) {
        mRemoteLogger.d(TAG + " onCallMediaAvailable");
        try {
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
        mRemoteLogger.d(TAG + " onCallStartRingback");
        mSipService.startRingback();
    }

    public void onCallStopRingback() {
        mRemoteLogger.d(TAG + " onCallStopRingback");
        mSipService.stopRingback();
    }
}

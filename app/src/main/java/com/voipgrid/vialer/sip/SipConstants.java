package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.BuildConfig;

/**
 * Interface containing Sip settings or event keys.
 */
public interface SipConstants {
    String PACKAGE_NAME = BuildConfig.APPLICATION_ID;

    /**
     * ACTION parameter for explicit Intents to communicate Service information with a view through
     * Broadcasts.
     */
    String ACTION_BROADCAST_CALL_STATUS = PACKAGE_NAME + ".CALL_STATUS";

    /**
     * ACTION parameter for explicit Intents to communicate Service information with a view through
     * Broadcasts.
     */
    String ACTION_BROADCAST_SERVICE_INFO = PACKAGE_NAME + ".SERVICE_INFO";

    String ACTION_BROADCAST_CALL_MISSED = PACKAGE_NAME + ".CALL_MISSED";

    /**
     * CallInteraction broadcast key for sending service info.
     */
    String SERVICE_INFO_KEY = "SERVICE_INFO";

    /**
     * CallStatus interface messages for communication with activity when Media for a call becomes
     * available.
     */
    String CALL_MEDIA_AVAILABLE_MESSAGE   = "MEDIA_AVAILABLE";

    /**
     * CallStatus interface messages for communication with activity when a call is Disconnected.
     */
    String CALL_DISCONNECTED_MESSAGE      = "DISCONNECTED";

    /**
     * CallStatus interface messages for communication with activity when a call is Connected.
     */
    String CALL_CONNECTED_MESSAGE         = "CONNECTED";

    /**
     * CallStatus interface messages for communication with activity when the app is ringing out.
     */
    String CALL_RINGING_OUT_MESSAGE       = "RINGING_OUT";

    /**
     * CallStatus interface messages for communication with activity when the app is ringing in.
     */
    String CALL_RINGING_IN_MESSAGE        = "RINGING_IN";

    String CALL_MUTED                     = "MUTED";

    String CALL_UNMUTED                   = "UNMUTED";

    /**
     * CallInteraction broadcast type for communicating a "Put on hold" to the SIP service through
     * Broadcasts.
     */
    String CALL_PUT_ON_HOLD_ACTION = "PUT_ON_HOLD";

    /**
     * Key used to broadcast current SIPCall status management to CallActivity to update view.
     */
    String CALL_STATUS_KEY = "call_status";

    /**
     * Key used to broadcast current SIPCall status code to CallActivity to update view.
     */
    String CALL_STATUS_CODE = "call_status_code";

    /**
     * Key used to broadcast current SIPCall status management to CallActivity to update view.
     */
    String CALL_IDENTIFIER_KEY = "call_identifier";

    String CALL_MISSED_KEY = "call_missed";

    /**
     * CallInteraction broadcast type for communicating a "UNHOLD" to the SIP service through
     * Broadcasts.
     */
    String CALL_UNHOLD_ACTION = "UNHOLD_CALL";

    String CALL_INCOMING_RINGING = "CALL_RINGING";
    String CALL_DECLINE_INCOMING_CALL = "CALL_DECLINE_CALL";
    String CALL_ANSWER_INCOMING_CALL = "CALL_ANSWER_CALL";

    String EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME";
    String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";

    String EXTRA_RESPONSE_URL = "EXTRA_RESPONSE_URL";
    String EXTRA_REQUEST_TOKEN = "EXTRA_REQUEST_TOKEN";

    String SERVICE_STOPPED = "SERVICE_STOPPED";

    String CALL_INVALID_STATE = "CALL_INVALID_STATE";
    String CALL_MEDIA_FAILED = "CALL_MEDIA_FAILED";
    String CALL_UPDATE_MICROPHONE_VOLUME_FAILED = "CALL_UPDATE_MICROPHONE_VOLUME_FAILED";

    enum CallMissedReason {
        UNKNOWN("UNKNOWN"),
        CALL_ORIGINATOR_CANCEL("ORIGINATOR_CANCEL"),
        CALL_COMPLETED_ELSEWHERE("Call completed elsewhere");

        private String stringValue;
        CallMissedReason(String toString) {
            stringValue = toString;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    // Volume for the ringing tone on a scale of 0 - 100.
    int RINGING_VOLUME = 75;

    // Echo cancellation.
    int WEBRTC_ECHO_CANCELLATION = 3;
    int ECHO_CANCELLATION_TAIL_LENGTH = 75;

    // Input verbosity level. Value 5 is reasonable.
    int SIP_LOG_LEVEL = 10;
    // For PJSIP debugging purpose 4 is a reasonable value.
    int SIP_CONSOLE_LOG_LEVEL = 4;

    /**
     * The duration of the tone to play when the remote party hangs up the call.
     *
     */
    int BUSY_TONE_DURATION = 2000;
}

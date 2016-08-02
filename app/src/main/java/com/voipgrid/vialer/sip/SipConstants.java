package com.voipgrid.vialer.sip;

/**
 * Interface containing Sip settings or event keys.
 */
public interface SipConstants {

    /**
     * ACTION parameter for explicit Intents to start this Service for OUTGOING call.
     */
    String ACTION_VIALER_OUTGOING = "com.voipgrid.vialer.VIALER_OUTGOING";

    /**
     * ACTION parameter for explicit Intents to start this Service for INCOMING call.
     */
    String ACTION_VIALER_INCOMING = "com.voipgrid.vialer.VIALER_INCOMING";

    /**
     * ACTION parameter for explicit Intents to communicate Service information with a view through
     * Broadcasts.
     */
    String ACTION_BROADCAST_CALL_STATUS = "com.voipgrid.vialer.VIALER_CALL_STATUS";

    /**
     * ACTION parameter for explicit Intents to communicate Service information with a view through
     * Broadcasts.
     */
    String ACTION_BROADCAST_SERVICE_INFO = "com.voipgrid.vialer.VIALER_SERVICE_INFO";

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
     * CallStatus interface messages for communication with activity when a call is Disconnected.
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

    /**
     * CallInteraction broadcast type for communicating a "Put on hold" to the SIP service through
     * Broadcasts.
     */
    String CALL_PUT_ON_HOLD_ACTION = "PUT_ON_HOLD";

    /**
     * Key used to broadcast current SIPCall status management to CalActivity to update view.
     */
    String CALL_STATUS_KEY = "call_status";

    /**
     * Key used to broadcast current SIPCall status management to CalActivity to update view.
     */
    String CALL_IDENTIFIER_KEY = "call_identifier";

    /**
     * CallInteraction broadcast type for communicating a "UNHOLD" to the SIP service through
     * Broadcasts.
     */
    String CALL_UNHOLD_ACTION = "UNHOLD_CALL";

    String EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME";
    String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";

    String EXTRA_RESPONSE_URL = "EXTRA_RESPONSE_URL";
    String EXTRA_REQUEST_TOKEN = "EXTRA_REQUEST_TOKEN";

    String SERVICE_STOPPED = "SERVICE_STOPPED";

    String CALL_INVALID_STATE = "CALL_INVALID_STATE";
    String CALL_MEDIA_FAILED = "CALL_MEDIA_FAILED";
    String CALL_UPDATE_MICROPHONE_VOLUME_FAILED = "CALL_UPDATE_MICROPHONE_VOLUME_FAILED";

    // Volume for the ringing tone on a scale of 0 - 100.
    int RINGING_VOLUME = 75;

    // Echo cancellation.
    int WEBRTC_ECHO_CANCELLATION = 3;
    int ECHO_CANCELLATION_TAIL_LENGTH = 75;

    // Input verbosity level. Value 5 is reasonable.
    int SIP_LOG_LEVEL = 10;
    // For PJSIP debugging purpose 4 is a reasonable value.
    int SIP_CONSOLE_LOG_LEVEL = 4;
}

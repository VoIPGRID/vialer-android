package com.voipgrid.vialer.sip;

/**
 * Created by eltjo on 02/09/15.
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
     * ACTION parameter for explicit Intents to communicate Service information with a view through Broadcasts.
     */
    String ACTION_BROADCAST_CALL_STATUS = "com.voipgrid.vialer.VIALER_CALL_STATUS";

    /**
     * CallStatus interface messages for communication with activity when Media for a call becomes available.
     */
    String CALL_MEDIA_AVAILABLE_MESSAGE   = "MEDIA_AVAILABLE";

    /**
     * CallStatus interface messages for communication with activity when Media for a call becomes unavailable.
     */
    String CALL_MEDIA_UNAVAILABLE_MESSAGE = "MEDIA_UNAVAILABLE";

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
     * CallInteraction broadcast action to communicate from Activity to service through Broadcasts.
     */
    String ACTION_BROADCAST_CALL_INTERACTION = "com.voipgrid.vialer.VIALER_CALL_INTERACTION";

    /**
     * SIPCall status Data key for communication of action on call to Service.
     */
    String CALL_STATUS_ACTION = "service_status";

    /**
     * CallInteraction broadcast type for communicating a "hang-up" to the SIP service through Broadcasts.
     */
    String CALL_HANG_UP_ACTION = "HANG_UP";

    /**
     * CallInteraction broadcast type for communicating a "Mute microphone" to the SIP service through Broadcasts.
     */
    String CALL_UPDATE_MICROPHONE_VOLUME_ACTION = "MUTE_MICROPHONE";

    /**
     * CallInteraction broadcast type for communicating a "Put on hold" to the SIP service through Broadcasts.
     */
    String CALL_PUT_ON_HOLD_ACTION = "PUT_ON_HOLD";

    /**
     * CallInteraction broadcast type for communicating a "Transfer" to the SIP service through Broadcasts.
     */
    String CALL_XFER_ACTION = "XFER";

    /**
     * Key used to pass extra data to sip service for microphone volume change.
     */
    String MICROPHONE_VOLUME_KEY = "MICROPHONE_VOLUME_EXTRA_KEY";

    /**
     * Key used to broadcast current SIPCall status management to CalActivity to update view.
     */
    String CALL_STATUS_KEY = "call_status";

    /**
     * CallInteraction broadcast type for communicating a "pick-up" to the SIP service through Broadcasts.
     */
    String CALL_PICK_UP_ACTION = "PICK_UP";

    /**
     * CallInteraction broadcast type for communicating a "decline" to the SIP service through Broadcasts.
     */
    String CALL_DECLINE_ACTION = "DECLINE";

    /**
     * CallInteraction broadcast type for communicating a "UNHOLD" to the SIP service through Broadcasts.
     */
    String CALL_UNHOLD_ACTION = "UNHOLD_CALL";

    /**
     * CallInteraction broadcast type for communicating amn optional "RINGBACK" to the SIP service through Broadcasts.
     */
    String CALL_START_RINGBACK_MESSAGE = "START_RINGBACK";

    /**
     * CallInteraction broadcast type for communicating amn optional "RINGBACK" to the SIP service through Broadcasts.
     */
    String CALL_STOP_RINGBACK_MESSAGE = "STOP_RINGBACK";

    String EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME";
    String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";

    String EXTRA_RESPONSE_URL = "EXTRA_RESPONSE_URL";
    String EXTRA_REQUEST_TOKEN = "EXTRA_REQUEST_TOKEN";

    String SERVICE_STOPPED = "SERVICE_STOPPED";

    String SIP_SERVICE_HAS_NO_ACCOUNT = "SIP_SERVICE_HAS_NO_ACCOUNT";
    String SIP_SERVICE_ACCOUNT_REGISTRATION_FAILED = "SIP_SERVICE_ACCOUNT_REGISTRATION_FAILED";
    String SIP_SERVICE_CAN_NOT_LOAD_PJSIP = "SIP_SERVICE_CAN_NOT_LOAD_PJSIP";
    String SIP_SERVICE_CAN_NOT_START_PJSIP = "SIP_SERVICE_CAN_NOT_START_PJSIP";
    String CALL_INVALID_STATE = "CALL_INVALID_STATE";
    String CALL_MEDIA_FAILED = "CALL_MEDIA_FAILED";
    String CALL_UPDATE_MICROPHONE_VOLUME_FAILED = "CALL_UPDATE_MICROPHONE_VOLUME_FAILED";
    String CALL_PUT_ON_HOLD_FAILED = "CALL_PUT_ON_HOLD_FAILED";

    String KEY_PAD_DTMF_TONE = "KEY_PAD_DTMF_TONE";

    String ACTION_BROADCAST_KEY_PAD_INTERACTION = "com.voipgrid.vialer.VIALER_KEY_PAD_INTERACTION";
}

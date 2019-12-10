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

    /**
     * CallInteraction broadcast key for sending service info.
     */
    String SERVICE_INFO_KEY = "SERVICE_INFO";



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


    String SERVICE_STOPPED = "SERVICE_STOPPED";


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

package com.voipgrid.vialer.logging.sip;

import android.content.Intent;

import com.voipgrid.vialer.VialerApplication;

/**
 * Performs various actions based on the result of the pjsip logs.
 */
public class SipLogHandler {

    private static final String  NETWORK_UNAVAILABLE = "Error sending RTP: Network is unreachable";

    public static final String NETWORK_UNAVAILABLE_BROADCAST = "com.voipgrid.vialer.logging.sip.NETWORK_UNAVAILABLE_BROADCAST";

    /**
     * Perform various tasks based on pjsip logs.
     *
     * @param log
     */
    public void handle(String log) {
        if(log == null) return;

        if(log.contains(NETWORK_UNAVAILABLE)) {
            performNetworkSwitch();
        }
    }

    /**
     * When pjsip is reporting that the network is unreachable, we will send out a broadcast so that
     * the IP can be updated and RTP can be resumed.
     *
     */
    private void performNetworkSwitch() {
        VialerApplication.get().sendBroadcast(new Intent(SipLogHandler.NETWORK_UNAVAILABLE_BROADCAST));
    }

}

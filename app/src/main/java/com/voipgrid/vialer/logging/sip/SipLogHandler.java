package com.voipgrid.vialer.logging.sip;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Performs various actions based on the result of the pjsip logs.
 */
public class SipLogHandler {

    private static final String NETWORK_UNAVAILABLE = "Error sending RTP: Network is unreachable";

    public static final String NETWORK_UNAVAILABLE_BROADCAST = "com.voipgrid.vialer.logging.sip.NETWORK_UNAVAILABLE_BROADCAST";

    public static final String INVITE_FAILED_WITH_SIP_ERROR_CODE = "com.voipgrid.vialer.logging.sip.INVITE_FAILED_WITH_SIP_ERROR_CODE";

    public static final String EXTRA_SIP_ERROR_CODE = "com.voipgrid.vialer.logging.sip.EXTRA_SIP_ERROR_CODE";

    /**
     * Any status codes in this list will not count as failure status codes even if they are 4xx or 5xx as
     * they are standard messages.
     *
     */
    private static final int[] IGNORED_STATUS_CODES = { 407 };

    /**
     * Perform various tasks based on pjsip logs.
     *
     * @param log
     */
    public void handle(String log) {
        if (log == null) return;

        if (log.contains(NETWORK_UNAVAILABLE)) {
            performNetworkSwitch();
        }

        if (isInvite(log)) {
            Integer inviteFailedCode = extractFailedInviteCode(log);

            if (inviteFailedCode != null && !Arrays.asList(IGNORED_STATUS_CODES).contains(inviteFailedCode)) {
                sendInviteFailedBroadcast(inviteFailedCode);
            }
        }
    }

    /**
     * Send a broadcast to let listeners know that a failed invite was detected.
     *
     * @param inviteFailedCode
     */
    private void sendInviteFailedBroadcast(Integer inviteFailedCode) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(VialerApplication.get());
        Intent intent = new Intent(SipLogHandler.INVITE_FAILED_WITH_SIP_ERROR_CODE);
        intent.putExtra(EXTRA_SIP_ERROR_CODE, inviteFailedCode);
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Checks to see if the packet being inspected is an INVITE with a failed status code.
     *
     * @param log
     * @return
     */
    private Integer extractFailedInviteCode(String log) {
        ArrayList<String> matches = StringUtil.extractCaptureGroups(log, "([0-9]{3})\\/([A-Z]+)");

        if (matches.isEmpty()) {
            return null;
        }

        String code = matches.get(0);
        String method = matches.get(1);

        if (!method.equals("INVITE")) {
            return null;
        }

        if (!code.startsWith("4") && !code.startsWith("5")) {
            return null;
        }

        return Integer.valueOf(code);
    }

    private boolean isInvite(String log) {
        return log.contains("INVITE");
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

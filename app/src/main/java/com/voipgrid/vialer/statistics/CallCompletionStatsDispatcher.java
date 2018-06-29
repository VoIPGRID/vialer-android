package com.voipgrid.vialer.statistics;

import com.voipgrid.vialer.media.monitoring.PacketStats;
import com.voipgrid.vialer.sip.SipCall;

public class CallCompletionStatsDispatcher {

    /**
     * Inspects what has happened during the call and sends the relevant metrics.
     *
     * @param call
     */
    public void callDidComplete(SipCall call) {
        PacketStats callPacketStats = call.getMediaPacketStats();

        if (callPacketStats == null) {
            return;
        }

        if (callPacketStats.hasTwoWayAudio()) {
            return;
        }

        VialerStatistics.callFailedDueToNoAudio(call, !callPacketStats.isMissingInboundAudio(), !callPacketStats.isNotSendingAudio());
    }
}

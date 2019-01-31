package com.voipgrid.vialer.sip;


import org.pjsip.pjsua2.RtcpStat;
import org.pjsip.pjsua2.RtcpStreamStat;
import org.pjsip.pjsua2.StreamStat;

/**
 * Class to get the statics from the current call.
 */
class SipCallStats {

    private SipCallStats() {
    }

    /**
     * Calculate the bandwidth used in megabytes.
     *
     * @param sipCall SipCall the current call to calculate the MOS for.
     * @return float the amount of bandwidth used during the call.
     * @throws Exception when the StreamStat can't be found.
     */
    static float calculateBandwidthUsage(SipCall sipCall) throws Exception {
        StreamStat streamStat = sipCall.getStreamStat(0);
        RtcpStat rtcpStat = streamStat.getRtcp();
        RtcpStreamStat rtcpStreamRxStat = rtcpStat.getRxStat();

        // Divide to get MB's
        return 1024 * 1024 / rtcpStreamRxStat.getBytes();
    }
}

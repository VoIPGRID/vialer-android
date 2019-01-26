package com.voipgrid.vialer.sip;


import org.pjsip.pjsua2.RtcpStat;
import org.pjsip.pjsua2.RtcpStreamStat;
import org.pjsip.pjsua2.StreamStat;

import static java.lang.Math.exp;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import android.util.Log;

import com.voipgrid.vialer.sip.mos.MosCalculator;

/**
 * Class to get the statics from the current call.
 */
class SipCallStats {

    private SipCallStats() {
        // Private constructor do disallow initializing this class.
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

    /**
     * Calculate the Mean-Opinion-Score (MOS) of the call
     *
     * @param sipCall SipCall the current call to calculate the MOS for.
     *
     * @return double MOS value between 0 - 5.
     *
     * @throws Exception when the StreamStat can't be found.
     */
    static double calculateMOS(SipCall sipCall) throws Exception {
        Log.e("TEST123", "MOS=" + new MosCalculator().calculate(sipCall));
        return 5.0;
    }

}

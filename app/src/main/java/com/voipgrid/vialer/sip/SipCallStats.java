package com.voipgrid.vialer.sip;


import org.pjsip.pjsua2.RtcpStat;
import org.pjsip.pjsua2.RtcpStreamStat;
import org.pjsip.pjsua2.StreamStat;

import static java.lang.Math.exp;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Class to get the statics from the current call.
 */
class SipCallStats {

    // Send loudness rating (dB)
    private static double SLR    = 8.0;

    // Receive loudness rating (dB)
    private static double RLR    = 2.0;

    // Sidetone masking rating (dB)
    private static double STMR   = 15.0;

    // D-Value of telephone, send side
    private static double Ds     = 3.0;

    // D-Value of telephone, receiver side
    private static double Dr     = 3.0;

    // Talker echo loudness rating (ms)
    private static double TELR   = 65.0;

    // Weighted echo path loss (ms)
    private static double WEPL   = 110.0;

    // Mean one-way delay of the echo path (ms)
    private static double T      = 0.0;

    // Round-trip delay in a 4-wire loop (ms)
    private static double Tr     = 0.0;

    // Absolute delay in echo-free connections (ms)
    private static double Ta     = 0.0;

    // Delay sensitivity
    private static double sT     = 1.0;

    // Minimum perceivable delay (ms)
    private static double mT     = 100.0;

    // Number of quantization distortion units
    private static double qdu    = 1.0;

    // Circuit noise referred to 0 dBr-point (dBm0p)
    private static double Nc     = -70.0;

    // Noise floor at the receive side (dBm0p)
    private static double Nfor   = -64.0;

    // Room noise at the send side (db(A))
    private static double Ps     = 35.0;

    // Room noise at the receive side (db(A))
    private static double Pr     = 35.0;

    // Listener sidetone rating (dB)
    private static double LSTR   = Dr + STMR;
    private static double OLR    = SLR + RLR;

    // Basic signal-to-noise ratio.
    private static double Ro;
    private static double Ist;
    private static double No;

    /**
     * Some iLBC default values.
     */
    private static double iLBCCodecImpairment   = 11.0;
    private static double iLBCBPL               = 32.0;
    private static double iLBCCodecFrameSize    = 25.0;
    private static double iLBCCodecPacketSize   = 30.0;

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
        StreamStat streamStat = sipCall.getStreamStat(0);

        setCodecValues(streamStat);

        double Ro = calculateSignalToNoiseRatio();
        double Is = calculateSimultaneousImpairments();
        double Id = calculateDelayImpairmentFactor();
        double Ie = calculateEquipmentImpairment(streamStat);

        double R = Ro - Is - Id - Ie;
        double MOS;

        if (R > 100) {
            MOS = 4.5;
        } else {
            if (R > 0) {
                MOS = 1 + R * 0.035 + R * (R - 60.0) * (100.0 - R) * 7.0 * pow(10.0, -6.0);
            } else {
                MOS = 0.0f;
            }
        }
        return MOS;
    }

    /**
     * Calculate the delay impairment factor, Id.
     *
     * Representing all the impairments due to delay of voice signals.
     *
     * @return double the value of the delay impairment (Id)
     */
    private static double calculateDelayImpairmentFactor() {
        double Rle = 10.5 * (WEPL + 7) * pow(Tr + 1, -0.25);
        double X;

        if (Ta == 0.0) {
            X = 0.0;
        } else {
            X = log10(Ta / 100.0) / log10(2);
        }

        double Idd;
        if (Ta <= 100.0) {
            Idd = 0;
        } else {
            Idd = 25.0f * (pow(1.0f + pow(X, 6.0f), 1.0f / 6.0f ) -3.0f * pow(1.0f + pow(X / 3.0f, 6.0f), 1.0f / 6.0f) + 2.0f);
        }

        double Idle = (Ro - Rle) / 2.0f + sqrt((pow(Ro - Rle, 2.0f) / 4.0f) + 169);
        double TERV = TELR - 40.0f * log10((1.0f + T / 10.0f) / (1.0f + T / 150.0f)) + 6.0f * exp( -0.3f * pow(T, 2));
        double TERVs = TERV + (Ist / 2);
        double Roe = -1.5 * (No - RLR);

        double Re;
        if (STMR < 9.0f) {
            Re = 80 + 2.5 * (TERVs - 14);
        } else {
            Re = 80 + 2.5 * (TERV - 14);
        }

        double Idte;

        if (T < 1.0) {
            Idte = 0.0;
        } else {
            Idte = ((Roe - Re) / 2.0 + sqrt(pow(Roe - Re, 2) / 4.0f + 100.0) - 1.0) * (1.0 - exp(-T));
        }

        double Id;
        if (STMR > 20.0f) {
            double Idtes = sqrt((pow(Idte, 2)) + (pow(Ist, 2)));
            Id = Idtes + Idle + Idd;
        } else {
            Id = Idte + Idle + Idd;
        }

        return Id;
    }

    /**
     * Calculate the signal to noise radio, Ro.
     *
     * @return double the value of Ro
     */
    private static double calculateSignalToNoiseRatio() {
        double Nfo = Nfor + RLR;

        double Nos = Ps - SLR - Ds - 100.0f + 0.004 * pow((Ps - OLR - Ds - 14.0), 2.0);
        double Pre = Pr + 10 * log10(1.0 + pow(10.0, ((10.0 - LSTR) / 10.0f))) / log10(10.0);
        double Nor = RLR - 121.0 + Pre + 0.008 * pow(Pre - 35.0, 2.0);
        No = 10 * log10(
                pow(10, (Nc / 10.0)) +
                        pow(10, (Nos / 10.0)) +
                        pow(10, (Nor / 10.0)) +
                        pow(10, (Nfo / 10.0))
        );
        Ro = 15.0 - 1.5 * (SLR + No);

        return Ro;
    }

    /**
     * Calculate the simultaneous impairment factor, Is.
     *
     * This is the sum of all impairments which may occur more or less
     * simultaneously with the voice transmission.
     *
     * @return double the value of simultaneous impairment factor (Is)
     */
    private static double calculateSimultaneousImpairments() {
        double Q;
        if (qdu < 1.0) {
            Q = 37.0 - 15.0 * log10(1.0) / log10(10.0);
        } else {
            Q = 37.0 - 15.0 * log10(qdu) / log10(10.0);
        }
        double G = 1.07 + 0.258 * Q + 0.0602 * pow(Q, 2);
        double Z = 46.0 / 30.0 - G / 40.0;
        double Y = (Ro - 100.0) / 15.0 + 46.0 / 8.4 - G / 9.0;
        double Iq = 15.0 * log10(1 + pow(10, Y) + pow(10, Z));
        double STMRo = -10 * log10(pow(10, -STMR / 10.0) + exp(-T / 4.0) * pow(10, -TELR / 10.0));
        Ist = 12 * pow( 1 + pow( (STMRo - 13.0) / 6.0, 8), 1.0 / 8.0) -28 * pow( 1 + pow( (STMRo + 1) / 19.4, 35), 1.0 / 35.0) -13 * pow( 1 + pow( (STMRo - 3) / 33.0, 13), 1.0 / 13.0) + 29;
        double Xolr = OLR + 0.2 * (64.0 + No - RLR);
        double Iolr = 20 * (pow( 1 + pow(Xolr / 8.0, 8.0), 1.0 / 8.0) - Xolr / 8);

        double Is = Iolr + Ist + Iq;

        return Is;
    }

    /**
     * Calculate the equipment impairment factor, Ie.
     *
     * @param streamStat StreamStat the stream statistics of the current call.
     *
     * @return double the value of the equipment impairment (Ie)
     */
    private static double calculateEquipmentImpairment(StreamStat streamStat) {
        double codecImpairment = iLBCCodecImpairment;
        double bpl = iLBCBPL;
        double burstR = streamStat.getJbuf().getAvgBurst();
        double rxPackets = streamStat.getRtcp().getRxStat().getPkt();
        double txPackets = streamStat.getRtcp().getTxStat().getPkt();
        double rxPacketLoss = streamStat.getRtcp().getRxStat().getLoss();
        double txPacketLoss = streamStat.getRtcp().getTxStat().getLoss();
        double rxPacketLossPercentage = rxPackets == 0 ? 100.0 : (rxPacketLoss / rxPackets) * 100.0f;
        double txPacketLossPercentage = txPackets == 0 ? 100.0 : (txPacketLoss / txPackets) * 100.0f;
        double ppl = rxPacketLossPercentage + txPacketLossPercentage;
        double Ie = codecImpairment + (95 - codecImpairment) * (ppl / (ppl / burstR + bpl));

        return Ie;
    }

    /**
     * Calculate the the values for:
     * - Mean one-way delay of the echo path (T)
     * - Round-trip delay in a 4-wire loop (Tr)
     * - Absolute delay in echo-free connections (Ts)
     *
     * Based on the The Prognosis model
     */
    private static void setCodecValues(StreamStat streamStat) {
        RtcpStat rtcpStat = streamStat.getRtcp();
        RtcpStreamStat rtcpStreamRxStat = rtcpStat.getRxStat();
        RtcpStreamStat rtcpStreamTxStat = rtcpStat.getTxStat();

        double packetSize = iLBCCodecPacketSize;
        double frameSize = iLBCCodecFrameSize;
        double codecVariant = 5;
        double jitter = rtcpStreamRxStat.getJitterUsec().getMean() / 1000.0 + rtcpStreamTxStat.getJitterUsec().getMean() / 1000.0;

        double Towtd = rtcpStat.getRttUsec().getMean() / 1000.0 / 2.0;
        double Tenc = (packetSize + 0.2f * frameSize) + codecVariant;
        double Tdec = frameSize + jitter;

        T = Tenc + Towtd + Tdec;
        Tr = Tenc + 2 * Towtd + Tdec;
        Ta = Tenc + Towtd + Tdec;
    }

}

package com.voipgrid.vialer.media.monitoring;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipCall;

import org.pjsip.pjsua2.CallOpParam;

/**
 * Responsible for analyzing the media packets sent/received during a call
 * and reporting errors and/or re-sending invites to correct any problem
 * that may have occurred.
 */
public class CallMediaMonitor implements Runnable {

    private final SipCall mSipCall;
    private final Logger mLogger;

    /**
     * Simply holds the last packet stats that have been fetched.
     *
     */
    private PacketStats mMostRecentPacketStats;

    /**
     * Track the packet stats at select intervals so we can send
     * reinvites if the audio appears to have stopped during a
     * call.
     */
    private PacketStats mLastTrackedStats;

    /**
     * Records the packet stats when a reinvite was attempted so we can see
     * if the reinvite has fixed the issue.
     */
    private PacketStats mPacketStatsWhenAttemptingReinvite;

    /**
     * The delay between checking the packet stats in milliseconds. This should not be changed
     * without updating the other timers.
     */
    private static final int QUERY_PACKET_STATS_INTERVAL_S = 1;

    /**
     * The frequency at which the current packet stats for a call should
     * be reported to the logger, this occurs when there is audio, in seconds. This
     * will not cause stats to be reported any quicker than {@value QUERY_PACKET_STATS_INTERVAL_S}.
     */
    private static final int REPORT_PACKET_STATS_EVERY_S = 10;

    /**
     * A reinvite will be sent if there is no audio detected in this
     * interval.
     */
    private static final int CHECK_FOR_NO_AUDIO_IN_PREVIOUS_S = 5;

    public CallMediaMonitor(SipCall sipCall) {
        mSipCall = sipCall;
        mLogger = new Logger(this.getClass());
    }

    @Override
    public void run() {
        while (shouldBeMonitoringMedia()) {
            mMostRecentPacketStats = mSipCall.getMediaPacketStats();

            if (mMostRecentPacketStats == null) break;

            handleMediaPacketStats(mMostRecentPacketStats);

            sleep(QUERY_PACKET_STATS_INTERVAL_S * 1000);
        }
    }

    /**
     * Analyse the packet stats and either log information about them or
     * send a re-invite if there is no audio at all.
     */
    private void handleMediaPacketStats(PacketStats packetStats) {
        // If all audio is missing from a call then send a re-invite
        if (packetStats.getCollectionTime() != 0 && packetStats.isEitherSideMissingAudio()) {
            mLogger.w("There is NO audio " + mSipCall.getCallDuration()
                    + " sec into the call. Trying a reinvite");
            attemptCallReinvite(packetStats);
            return;
        }

        if (shouldTrackThesePacketStats()) {
            PacketStats packetStatsForLastInterval = packetStats.difference(mLastTrackedStats);

            // If there has been no audio since our last interval this means that audio may have
            // dropped and we should send a reinvite.
            if (mLastTrackedStats != null && packetStatsForLastInterval.isMissingAllAudio()) {
                mLogger.w("There has been NO audio between "
                        + mLastTrackedStats.getCollectionTime() + "s and "
                        + packetStats.getCollectionTime() + "s. Trying a reinvite");
                attemptCallReinvite(packetStats);
            }

            mLastTrackedStats = packetStats;
        }

        // If we have previously attempted a reinvite, check if we have had any audio packets
        // since the reinvite, this will tell us if the reinvite worked.
        if (mPacketStatsWhenAttemptingReinvite != null) {
            PacketStats packetStatsSinceLastInvite = packetStats.difference(mPacketStatsWhenAttemptingReinvite);
            if (packetStatsSinceLastInvite.hasAudio()) {
                mLogger.i(packetStatsSinceLastInvite.getReceived() + "rx, " + packetStatsSinceLastInvite.getSent() + "tx packets have been detected after reinvite between " + mPacketStatsWhenAttemptingReinvite.getCollectionTime() + "s " + packetStats.getCollectionTime() + "s");
            }
            mPacketStatsWhenAttemptingReinvite = null;
        }

        if (packetStats.hasAudio() && isTimeToReportAudioStats()) {
            mLogger.i(
                    "There is audio in the last " + REPORT_PACKET_STATS_EVERY_S + " seconds rxPkt: "
                            + packetStats.getReceived() + " and txPkt: " + packetStats.getSent());
        }
    }

    /**
     * Attempts to send a reinvite for the call, if this fails then a message
     * is logged.
     * @param packetStats
     */
    private void attemptCallReinvite(PacketStats packetStats) {
        try {
            mSipCall.reinvite(new CallOpParam(true));
            mPacketStatsWhenAttemptingReinvite = packetStats;
        } catch (Exception e) {
            mLogger.e("Unable to reinvite call: " + e.getMessage());
        }
    }

    /**
     * Check if we should track the current packet stats, this means updating
     * the tracked stats
     *
     * @return TRUE if stats should be reported, otherwise false.
     */
    private boolean shouldTrackThesePacketStats() {
        return intervalShouldBeTriggered(CHECK_FOR_NO_AUDIO_IN_PREVIOUS_S);
    }

    /**
     * Check the current call duration to determine if we should be reporting stats or not,
     * it should be checked ever {@value REPORT_PACKET_STATS_EVERY_S} seconds.
     *
     * @return TRUE if stats should be reported, otherwise false.
     */
    private boolean isTimeToReportAudioStats() {
        return intervalShouldBeTriggered(REPORT_PACKET_STATS_EVERY_S);
    }

    /**
     * Returns TRUE if the current interval should be triggered based on the call duration.
     *
     * For example, an interval of 5 seconds should be triggered on 5, 10, 15, 20, 25, 30 etc.
     *
     * @param interval The interval to check for
     * @return TRUE if the interval is ready to be triggered
     */
    private boolean intervalShouldBeTriggered(int interval) {
        if (mSipCall.getCallDuration() == 0) {
            return false;
        }

        return (mSipCall.getCallDuration() % interval) == 0;
    }

    /**
     * Check that the call is still alive, if it is we should be monitoring it.
     *
     * @return TRUE if we should be monitoring the call media, otherwise false.
     */
    private boolean shouldBeMonitoringMedia() {
        return mSipCall != null && mSipCall.isConnected();
    }

    /**
     * Sleep the current thread for the given number of milliseconds.
     *
     * @param milliseconds The number of milliseconds to sleep the current thread for.
     */
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public PacketStats getMostRecentPacketStats() {
        return mMostRecentPacketStats;
    }
}

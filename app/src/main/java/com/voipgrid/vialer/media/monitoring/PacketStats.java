package com.voipgrid.vialer.media.monitoring;

import android.support.annotation.Nullable;

import com.voipgrid.vialer.sip.SipCall;

import org.pjsip.pjsua2.StreamStat;

public class PacketStats {

    private final long mSent;
    private final long mReceived;
    private final int mCollectionTime;

    /**
     * Create a new, immutable instance of PacketStats.
     *
     * @param sent The number of packets sent.
     * @param received The number of packets received.
     * @param collectionTime The time (relative to the call) when the stats were collected.
     */
    public PacketStats(long sent, long received, int collectionTime) {
        mSent = sent;
        mReceived = received;
        mCollectionTime = collectionTime;
    }

    /**
     * Returns the duration of the sip call when these stats were collected.
     *
     * @return
     */
    public int getCollectionTime() {
        return mCollectionTime;
    }

    /**
     * Get the number of packets that have been sent (the user's voice).
     *
     * @return The number of packets as a long
     */
    public long getSent() {
        return mSent;
    }

    /**
     * Get the number of packets that have been received (the third party's voice).
     *
     * @return The number of packets as a long
     */
    public long getReceived() {
        return mReceived;
    }

    /**
     * Check if there is some audio present at all.
     *
     * @return
     */
    public boolean hasAudio() {
        return !isMissingInboundAudio() || !isNotSendingAudio();
    }

    /**
     * Check if any media has been transmitted for this call.
     *
     * @return If any media has been sent or received TRUE, otherwise FALSE.
     */
    public boolean isMissingAllAudio() {
        return isMissingInboundAudio() && isNotSendingAudio();
    }

    /**
     * Determine if at least one side is lacking media, if TRUE is returned this would
     * suggest there is a problem with the call.
     *
     * @return If sent or received have not transmitted any packets.
     */
    public boolean isEitherSideMissingAudio() {
        return isMissingInboundAudio() || isNotSendingAudio();
    }

    /**
     * Check if we have not received any audio at all.
     *
     * @return TRUE if the number of received packets is 0
     */
    public boolean isMissingInboundAudio() {
        return getReceived() == 0;
    }

    /**
     * Check if we have not sent any audio at all.
     *
     * @return TRUE if the number of sent packets is 0
     */
    public boolean isNotSendingAudio() {
        return getSent() == 0;
    }

    /**
     * Get the difference between two PacketStats objects as a new PacketStats
     * object.
     *
     * @return
     */
    public PacketStats difference(PacketStats previous) {
        if (previous == null) {
            return new PacketStats(0, 0,0);
        }

        return new PacketStats(
                this.getSent() - previous.getSent(),
                this.getReceived() - previous.getReceived(),
                this.getCollectionTime()
        );
    }

    @Override
    public String toString() {
        return "PacketStats{" +
                "mSent=" + mSent +
                ", mReceived=" + mReceived +
                ", collectionTime=" + mCollectionTime +
                '}';
    }

    public static class Builder {

        /**
         * Builds a packet stats object from a sip call.
         *
         * @param sipCall
         * @return The complete packet stats
         */
        public static @Nullable PacketStats fromSipCall(SipCall sipCall) {
            try {
                StreamStat streamStat = sipCall.getStreamStat(sipCall.getId());

                if (streamStat == null) return null;

                return new PacketStats(
                        streamStat.getRtcp().getTxStat().getPkt(),
                        streamStat.getRtcp().getRxStat().getPkt(),
                        sipCall.getCallDuration()
                );
            } catch (Throwable e) {
                return null;
            }
        }
    }
}

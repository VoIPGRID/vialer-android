package com.voipgrid.vialer.calling;

import android.os.Handler;

import com.voipgrid.vialer.sip.SipService;

public class CallDurationTracker implements Runnable {

    /**
     * The interval at which we check for updates in the call
     * duration.
     */
    private static final int INTERVAL_MS = 500;

    private static Handler sHandler = new Handler();

    private final SipServiceConnection mSipServiceConnection;
    private Listener mListener;

    CallDurationTracker(SipServiceConnection sipServiceConnection) {
        mSipServiceConnection = sipServiceConnection;
    }

    public void start(CallDurationTracker.Listener listener) {
        mListener = listener;
        sHandler.postDelayed(this, 0);
    }

    public void stop() {
        sHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        sHandler.removeCallbacks(this);

        if (!mSipServiceConnection.isAvailableOrIsAvailableSoon()) {
            return;
        }

        if (hasActiveCalls(mSipServiceConnection)) {
            long duration = findCallDuration();

            if (duration < 0) {
                stop();
                return;
            }

            mListener.onCallDurationUpdate(duration);
        }

        sHandler.postDelayed(this, INTERVAL_MS);
    }

    /**
     * Find the most relevant call duration.
     *
     * @return
     */
    private long findCallDuration() {
        SipService sipService = mSipServiceConnection.get();

        if (sipService.getCurrentCall() == null) {
            return 0L;
        }

        if (sipService.getCurrentCall() == null) {
            return sipService.getCurrentCall().getDuration();
        }

        return sipService.getCurrentCall().getDuration();
    }

    /**
     * Checks the SipService to see if it is active, and if it has any calls that we can find the duration for.
     *
     * @param sipServiceConnection
     * @return
     */
    private boolean hasActiveCalls(SipServiceConnection sipServiceConnection) {
        return sipServiceConnection.isAvailable() && (
                sipServiceConnection.get().getCurrentCall() != null
                        || sipServiceConnection.get().getCurrentCall() != null);

    }

    /**
     * Interface to receive updates from the call duration tracker.
     *
     */
    public interface Listener {
        void onCallDurationUpdate(long duration);
    }
}

package com.voipgrid.vialer.calling;

import android.os.Handler;

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
            mListener.onCallDurationUpdate(findCallDuration());
        }

        sHandler.postDelayed(this, INTERVAL_MS);
    }

    /**
     * Find the most relevant call duration.
     *
     * @return
     */
    private long findCallDuration() {
        String firstCallIdentifier = mSipServiceConnection.get().getFirstCall().getIdentifier();

        if (mSipServiceConnection.get().getCurrentCall() == null) {
            return mSipServiceConnection.get().getFirstCall().getCallDuration();
        }

        String currentCallIdentifier = mSipServiceConnection.get().getCurrentCall().getIdentifier();

        if (firstCallIdentifier.equals(currentCallIdentifier)) {
            return mSipServiceConnection.get().getFirstCall().getCallDuration();
        }

        return mSipServiceConnection.get().getCurrentCall().getCallDuration();
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
                        || sipServiceConnection.get().getFirstCall() != null);

    }

    /**
     * Interface to receive updates from the call duration tracker.
     *
     */
    public interface Listener {
        void onCallDurationUpdate(long duration);
    }
}

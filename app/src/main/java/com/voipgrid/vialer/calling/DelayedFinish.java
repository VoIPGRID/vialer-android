package com.voipgrid.vialer.calling;

import android.app.Activity;
import android.os.Handler;

import com.voipgrid.vialer.logging.Logger;

public class DelayedFinish implements Runnable {
    private final Activity mActivity;
    private final Handler mHandler;
    private final SipServiceConnection mSipServiceConnection;
    private Handler delayedHandler = new Handler();
    private Logger mLogger;

    private static final int DELAYED_FINISH_MS = 3000;
    private static final int DELAYED_FINISH_RETRY_MS = 1000;

    DelayedFinish(Activity activity, Handler handler, SipServiceConnection sipServiceConnection) {
        mActivity = activity;
        mHandler = handler;
        mSipServiceConnection = sipServiceConnection;
        mLogger = new Logger(this.getClass());
    }

    public void begin() {
        this.delayedHandler.removeCallbacks(this);
        this.delayedHandler.postDelayed(this, DELAYED_FINISH_RETRY_MS);
    }

    @Override
    public void run() {
        if(mSipServiceConnection.hasActiveCall()) {
            mLogger.i("Call is still active " + DELAYED_FINISH_MS + "ms after finishWithDelay was called, trying again in " + DELAYED_FINISH_RETRY_MS + "ms");
            this.delayedHandler.removeCallbacks(this);
            this.delayedHandler.postDelayed(this, DELAYED_FINISH_RETRY_MS);
            return;
        }

        mActivity.finish();
    }
}

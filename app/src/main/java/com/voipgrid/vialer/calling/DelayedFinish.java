package com.voipgrid.vialer.calling;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.RemoteLogger;

public class DelayedFinish implements Runnable {
    private final Activity mActivity;
    private final Handler mHandler;
    private final SipServiceConnection mSipServiceConnection;
    private Handler delayedHandler = new Handler();
    private RemoteLogger mRemoteLogger;

    private static final int DELAYED_FINISH_MS = 3000;
    private static final int DELAYED_FINISH_RETRY_MS = 1000;

    DelayedFinish(Activity activity, Handler handler, SipServiceConnection sipServiceConnection) {
        mActivity = activity;
        mHandler = handler;
        mSipServiceConnection = sipServiceConnection;
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
    }

    public void begin() {
        this.delayedHandler.removeCallbacks(this);
        this.delayedHandler.postDelayed(this, DELAYED_FINISH_RETRY_MS);
    }

    @Override
    public void run() {
        if(mSipServiceConnection.hasActiveCall()) {
            mRemoteLogger.i("Call is still active " + DELAYED_FINISH_MS + "ms after finishWithDelay was called, trying again in " + DELAYED_FINISH_RETRY_MS + "ms");
            this.delayedHandler.removeCallbacks(this);
            this.delayedHandler.postDelayed(this, DELAYED_FINISH_RETRY_MS);
            return;
        }

        // Check to see if the call activity is the last activity.
        if (mActivity.isTaskRoot() && VialerApplication.get().isApplicationVisible()) {
            mRemoteLogger.i("There are no more activities, to counter an loop of starting CallActivity, start the MainActivity");
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mActivity.startActivity(intent);
        }

        mActivity.finish();
    }
}

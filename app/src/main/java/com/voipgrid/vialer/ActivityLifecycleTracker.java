package com.voipgrid.vialer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.notifications.EncryptionDisabledNotification;

public class ActivityLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private boolean isApplicationVisible = false;

    private static EncryptionDisabledNotification encryptionDisabledNotification;
    private CancelEncryptionNotification cancelEncryptionNotification = new CancelEncryptionNotification();
    private Handler handler = new Handler();

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        new Logger(activity.getClass()).d("onCreate");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        new Logger(activity.getClass()).d("onStart");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        isApplicationVisible = true;
        new Logger(activity.getClass()).d("onResume");

        if (!SecureCalling.fromContext(activity).isEnabled()) {
            handler.removeCallbacks(cancelEncryptionNotification);

            if (encryptionDisabledNotification == null) {
                encryptionDisabledNotification = new EncryptionDisabledNotification();
                encryptionDisabledNotification.display();
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isApplicationVisible = false;
        new Logger(activity.getClass()).d("onPause");

        new Handler().postDelayed(cancelEncryptionNotification, 1000);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        new Logger(activity.getClass()).d("onStop");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        new Logger(activity.getClass()).d("onDestroy");
    }

    public boolean isApplicationVisible() {
        return isApplicationVisible;
    }

    public static void removeEncryptionNotification() {
        if (encryptionDisabledNotification == null) return;

        encryptionDisabledNotification.remove();
        encryptionDisabledNotification = null;
    }

    private class CancelEncryptionNotification implements Runnable {

        @Override
        public void run() {
            if (encryptionDisabledNotification != null && !isApplicationVisible()) {
                encryptionDisabledNotification.remove();
                encryptionDisabledNotification = null;
            }
        }
    }
}

package com.voipgrid.vialer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;

import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.notifications.EncryptionDisabledNotification;
import com.voipgrid.vialer.onboarding.Onboarder;

public class ActivityLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private boolean isApplicationVisible = false;

    private static EncryptionDisabledNotification encryptionDisabledNotification;
    private CancelEncryptionNotification cancelEncryptionNotification = new CancelEncryptionNotification();
    private Handler handler = new Handler();

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        logActivityEvent(activity, "onCreate");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        logActivityEvent(activity, "onStart");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        isApplicationVisible = true;
        logActivityEvent(activity, "onResume");

        if (activity instanceof Onboarder) return;

        if (SecureCalling.fromContext(activity).hasBeenDisabled()) {
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
        logActivityEvent(activity, "onPause");

        new Handler().postDelayed(cancelEncryptionNotification, 1000);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        logActivityEvent(activity, "onStop");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        logActivityEvent(activity, "onDestroy");
    }

    private void logActivityEvent(Activity activity, String event) {
        new Logger(activity.getClass()).d(activity.getClass().getName() + " - " + event);
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

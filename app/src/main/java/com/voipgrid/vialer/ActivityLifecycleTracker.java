package com.voipgrid.vialer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.voipgrid.vialer.logging.Logger;

public class ActivityLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private boolean isApplicationVisible = false;

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
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isApplicationVisible = false;
        new Logger(activity.getClass()).d("onPause");
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
}

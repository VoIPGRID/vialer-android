package com.voipgrid.vialer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class ActivityLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private boolean isApplicationVisible = false;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        isApplicationVisible = true;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isApplicationVisible = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public boolean isApplicationVisible() {
        return isApplicationVisible;
    }
}

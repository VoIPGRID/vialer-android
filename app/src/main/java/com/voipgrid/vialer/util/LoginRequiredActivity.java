package com.voipgrid.vialer.util;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.onboarding.SetupActivity;

/**
 * Super class to handle logged in state of activity.
 */
public class LoginRequiredActivity extends AppCompatActivity {

    private boolean mHandlerIsSet = false;

    @Override
    protected void onResume() {
        super.onResume();

        if(!mHandlerIsSet) {
            // Setup handler for uncaught exceptions.
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                    handleUncaughtException(thread, e);
                }
            });
            mHandlerIsSet = true;
        }

        Preferences prefs = new Preferences(this);
        if (!prefs.isLoggedIn()) {
            new RemoteLogger(this).w("Not logged in anymore! Redirecting to onboarding");
            // Go to onboarding.
            Intent intent = new Intent(new Intent(this, SetupActivity.class));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    public boolean isUIThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public void handleUncaughtException(Thread thread, Throwable e) {
        e.printStackTrace();

        if (isUIThread()) {
            invokeLogActivity();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    invokeLogActivity();
                }
            });
        }
    }

    private void invokeLogActivity() {
        Intent intent = new Intent();
        intent.setAction("com.voipgrid.vialer.util.SEND_LOG");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        System.exit(1);
    }
}

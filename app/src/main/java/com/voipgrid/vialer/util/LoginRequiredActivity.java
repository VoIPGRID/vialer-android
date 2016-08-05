package com.voipgrid.vialer.util;

import android.content.Intent;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.logging.RemoteLoggingActivity;
import com.voipgrid.vialer.onboarding.SetupActivity;

/**
 * Super class to handle logged in state of activity.
 */
public class LoginRequiredActivity extends RemoteLoggingActivity {

    @Override
    protected void onResume() {
        super.onResume();

        Preferences prefs = new Preferences(this);
        if (!prefs.isLoggedIn() && prefs.finishedOnboarding()) {
            new RemoteLogger(this).w("Not logged in anymore! Redirecting to onboarding");
            // Go to onboarding.
            Intent intent = new Intent(new Intent(this, SetupActivity.class));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}

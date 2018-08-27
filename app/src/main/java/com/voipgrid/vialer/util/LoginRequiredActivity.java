package com.voipgrid.vialer.util;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.logging.RemoteLoggingActivity;
import com.voipgrid.vialer.onboarding.SetupActivity;

/**
 * Super class to handle logged in state of activity.
 */
public class LoginRequiredActivity extends RemoteLoggingActivity {

    private AccountHelper mAccountHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountHelper = new AccountHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Preferences prefs = new Preferences(this);
        if (!prefs.isLoggedIn()) {
            new RemoteLogger(LoginRequiredActivity.class).w("Not logged in anymore! Redirecting to onboarding");
            // Go to onboarding.
            Intent intent = new Intent(new Intent(this, SetupActivity.class));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    protected String getEmail() {
        return mAccountHelper.getEmail();
    }

    protected String getPassword() {
        return mAccountHelper.getPassword();
    }

    protected String getApiToken() {
        return mAccountHelper.getApiToken();
    }

    protected boolean hasApiToken() {
        return mAccountHelper.hasApiToken();
    }
}

package com.voipgrid.vialer.util;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.logging.VialerBaseActivity;
import com.voipgrid.vialer.onboarding.OnboardingActivity;

/**
 * Super class to handle logged in state of activity.
 */
public abstract class LoginRequiredActivity extends VialerBaseActivity {

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
        if (!prefs.isLoggedIn() && prefs.finishedOnboarding()) {
            new Logger(LoginRequiredActivity.class).w("Not logged in anymore! Redirecting to onboarding");
            // Go to onboarding.
            Intent intent = new Intent(new Intent(this, OnboardingActivity.class));
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

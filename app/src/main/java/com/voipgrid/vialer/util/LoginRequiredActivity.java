package com.voipgrid.vialer.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.onboarding.SetupActivity;

/**
 * Super class to handle logged in state of activity.
 */
public class LoginRequiredActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        Preferences prefs = new Preferences(this);
        if (!prefs.isLoggedIn()) {
            // Go to onboarding.
            Intent intent = new Intent(new Intent(this, SetupActivity.class));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}

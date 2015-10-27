package com.voipgrid.vialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.voipgrid.vialer.util.Middleware;

/**
 * Listen for updates in GCM instance id, and delegate them to the registration service.
 */
public class VialerInstanceIdListenerService extends InstanceIDListenerService implements Middleware.Constants {
    @Override
    public void onTokenRefresh() {
        /* Restart the Registration service to try and acquire a new token.
         * Before we do that, though, make sure the system knows our registration
         * is no longer valid. */
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(REGISTRATION_STATUS, STATUS_UNREGISTERED);
        editor.apply();
        Intent intent = new Intent(this, VialerGcmRegistrationService.class);
        startService(intent);
    }
}

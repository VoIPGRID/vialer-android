package com.voipgrid.vialer.fcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voipgrid.vialer.util.MiddlewareHelper;

/**
 * This class handles registration for GCM.
 */
public class FcmRegistrationService extends IntentService implements MiddlewareHelper.Constants {
    public static final String TAG = IntentService.class.getSimpleName();

    /* For some unfathomable reason IntentService requires this constructor */
    public FcmRegistrationService() {
        super(TAG);
    }

    /* It is possible the registration races. In which case, at least serialize registration
    * (as noted by the google gcm sample code). */
    @Override
    public synchronized void onHandleIntent(Intent intent) {
        /* Try to get a token and post it to the backend server */
        String token = FirebaseInstanceId.getInstance().getToken();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentToken = preferences.getString(CURRENT_TOKEN, "");

        // If token changed or we are not registered with the middleware register.
        if (token != null && (!token.equals(currentToken) || MiddlewareHelper.needsRegistration(this))) {
            MiddlewareHelper.register(this, token);
        }
    }
}

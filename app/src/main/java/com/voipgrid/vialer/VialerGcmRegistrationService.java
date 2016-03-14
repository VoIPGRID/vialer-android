package com.voipgrid.vialer;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.voipgrid.vialer.util.MiddlewareHelper;

import java.io.IOException;


/**
 * This class handles registration for GCM.
 */
public class VialerGcmRegistrationService extends IntentService implements MiddlewareHelper.Constants {
    public static final String TAG = IntentService.class.getSimpleName();

    /* For some unfathomable reason IntentService requires this constructor */
    public VialerGcmRegistrationService() {
        super(TAG);
    }

    /* It is possible the registration races. In which case, at least serialize registration
    * (as noted by the google gcm sample code). */
    @Override
    public synchronized void onHandleIntent(Intent intent) {
        /* Try to get a token and post it to the backend server */
        String token = getPushToken();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentToken = preferences.getString(CURRENT_TOKEN, "");

        // If token changed or we are not registered with the middleware register.
        if (token != null && (!token.equals(currentToken) || MiddlewareHelper.needsRegistration(this))) {
            MiddlewareHelper.register(this, token);
        }
    }

    /**
     * Function to get the push token from google.
     * @return
     */
    public String getPushToken() {
        String token = null;
        try {
            /* InstanceID is the 'new' interface to GCM token registration. */
            InstanceID iid = InstanceID.getInstance(this);
            /* This may connect to the internet and throw an IOException */

            token = iid.getToken(
                    getString(R.string.gcm_registration_id),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null
            );
        } catch (IOException e) {
            return token;
        }
        return token;
    }
}

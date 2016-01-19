package com.voipgrid.vialer;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.voipgrid.vialer.util.Middleware;

import java.io.IOException;


/**
 * This class handles registration for GCM.
 */
public class VialerGcmRegistrationService extends IntentService implements Middleware.Constants {
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
        try {
            /* InstanceID is the 'new' interface to GCM token registration. */
            InstanceID iid = InstanceID.getInstance(this);
            /* This may connect to the internet and throw an IOException */

            String token = iid.getToken(
                    getString(R.string.gcm_registration_id),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null
            );
            /* Send to server and save our registration status */
            Middleware.register(this, token);
        } catch (IOException exception) {
        }
    }
}

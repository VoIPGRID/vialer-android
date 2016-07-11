package com.voipgrid.vialer.fcm;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.voipgrid.vialer.util.MiddlewareHelper;

/**
 * Listen for updates in GCM instance id, and delegate them to the registration service.
 */
public class FcmInstanceIdListenerService extends FirebaseInstanceIdService
        implements MiddlewareHelper.Constants {

    @Override
    public void onTokenRefresh() {
        /* Restart the Registration service to try and acquire a new token.
         * Before we do that, though, make sure the system knows our registration
         * is no longer valid. */
        MiddlewareHelper.setRegistrationStatus(this, STATUS_UNREGISTERED);
        Intent intent = new Intent(this, FcmRegistrationService.class);
        startService(intent);
    }
}

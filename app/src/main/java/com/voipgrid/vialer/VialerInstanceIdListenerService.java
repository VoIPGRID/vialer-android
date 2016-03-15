package com.voipgrid.vialer;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.voipgrid.vialer.util.MiddlewareHelper;

/**
 * Listen for updates in GCM instance id, and delegate them to the registration service.
 */
public class VialerInstanceIdListenerService extends InstanceIDListenerService
        implements MiddlewareHelper.Constants {

    @Override
    public void onTokenRefresh() {
        /* Restart the Registration service to try and acquire a new token.
         * Before we do that, though, make sure the system knows our registration
         * is no longer valid. */
        MiddlewareHelper.setRegistrationStatus(this, STATUS_UNREGISTERED);
        Intent intent = new Intent(this, VialerGcmRegistrationService.class);
        startService(intent);
    }
}

package com.voipgrid.vialer.fcm;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;

import static com.voipgrid.vialer.middleware.MiddlewareConstants.STATUS_UNREGISTERED;

/**
 * Listen for updates in FCM instance id, and delegate them to the registration service.
 */
public class FcmInstanceIdListenerService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        new Logger(FcmInstanceIdListenerService.class).d("onTokenRefresh");
        // Make sure the system knows our registration is no longer valid.
        MiddlewareHelper.setRegistrationStatus(this, STATUS_UNREGISTERED);
        MiddlewareHelper.registerAtMiddleware(this);
    }
}

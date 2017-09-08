package com.voipgrid.vialer.middleware;


public final class MiddlewareConstants {

    private MiddlewareConstants() {
        // Restrict initializations.
    }

    public static final String REGISTRATION_STATUS = "VIALER_REGISTRATION_STATUS";
    public static final String LAST_REGISTRATION = "VIALER_LAST_REGISTRATION";
    public static final String CURRENT_TOKEN = "VIALER_CURRENT_TOKEN";

    // Not registered with the middleware.
    public static final int STATUS_UNREGISTERED = 0;
    // Registered with the middleware.
    public static final int STATUS_REGISTERED = 1;
    // Registration with the middleware failed.
    public static final int STATUS_FAILED = -1;
    // Registration with the middleware needs to be updated.
    public static final int STATUS_UPDATE_NEEDED = 2;
}

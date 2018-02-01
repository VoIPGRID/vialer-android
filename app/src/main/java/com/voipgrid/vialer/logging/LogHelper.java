package com.voipgrid.vialer.logging;

import com.voipgrid.vialer.sip.SipService;

public class LogHelper {

    private RemoteLogger mRemoteLogger;

    private LogHelper(RemoteLogger remoteLogger) {
        mRemoteLogger = remoteLogger;
    }

    public static LogHelper using(RemoteLogger remoteLogger) {
        return new LogHelper(remoteLogger);
    }

    /**
     * Creates a log when Vialer is responding with busy.
     *
     * @param mSipService
     */
    public void logBusyReason(SipService mSipService) {
        String message = "Responding with busy because: ";

        if(mSipService.getCurrentCall() != null) {
            message += "sip service has a call currently";
        }

        if(mSipService.getNativeCallManager().nativeCallIsInProgress()) {
            message += "there is a native call in progress";
        }

        if(mSipService.getNativeCallManager().nativeCallIsRinging()) {
            message += "there is a native call ringing";
        }

        mRemoteLogger.d(message);
    }
}

package com.voipgrid.vialer.logging;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.sip.SipService;

import java.util.HashMap;
import java.util.Map;

public class LogHelper {

    private RemoteLogger mRemoteLogger;
    private Gson mGson;

    private LogHelper(RemoteLogger remoteLogger) {
        mRemoteLogger = remoteLogger;
        mGson = new GsonBuilder().disableHtmlEscaping().create();
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

    /**
     * Sends a log to the push logging environment, used to track success rates of push messages.
     *
     * @param remoteMessage
     */
    public void logMiddlewareMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> message = new HashMap<>(remoteMessage.getData());

        message.put("caller_id", "<CALLER_ID>");
        message.put("phonenumber", "<PHONE_NUMBER>");

        mRemoteLogger.getVialerLogger().logToEnvironment(
                VialerApplication.get().getString(R.string.push_log_entries_token),
                "ANDROID : " + mGson.toJson(message)
        );
    }
}

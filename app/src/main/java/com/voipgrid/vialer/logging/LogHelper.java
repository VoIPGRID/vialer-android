package com.voipgrid.vialer.logging;


import static com.voipgrid.vialer.fcm.RemoteMessageData.CALL_REQUEST_TYPE;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.sip.SipService;

import java.util.Map;
import java.util.TreeMap;

public class LogHelper {

    private Logger mLogger;
    private Gson mGson;

    private LogHelper(Logger logger) {
        mLogger = logger;
        mGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    }

    public static LogHelper using(Logger logger) {
        return new LogHelper(logger);
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

        mLogger.d(message);
    }

    /**
     * Sends a log to the push logging environment, used to track success rates of push messages.
     *
     * @param remoteMessage
     * @param requestType
     */
    public void logMiddlewareMessageReceived(RemoteMessage remoteMessage, String requestType) {
        if(!CALL_REQUEST_TYPE.equals(requestType)) return;

        Map<String, String> message = new TreeMap<>(remoteMessage.getData());

        message.put("caller_id", "<CALLER_ID>");
        message.put("phonenumber", "<PHONE_NUMBER>");

        mLogger.getVialerLogger().logToEnvironment(
                VialerApplication.get().getString(R.string.push_log_entries_token),
                "ANDROID : " + mGson.toJson(message)
        );
    }

    /**
     * Log the appropriate message based on the reason for secure calling not being enabled.
     *
     */
    public void logNoTlsReason() {
        SecureCalling secureCalling = SecureCalling.fromContext(VialerApplication.get());

        if (!secureCalling.isEnabled()) {
            if (secureCalling.isSetCorrectly()) {
                mLogger.i("TLS will not be used for this call because the user has disabled it in advanced settings");
            } else {
                mLogger.w("TLS will not be used for this call because we have been unable to update the VoIP account");
            }
        }
    }
}

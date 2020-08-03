package com.voipgrid.vialer.logging;


import static com.voipgrid.vialer.fcm.RemoteMessageData.CALL_REQUEST_TYPE;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.SecureCalling;

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
}

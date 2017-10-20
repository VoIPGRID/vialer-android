package com.voipgrid.vialer.logging;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.logentries.logger.AndroidLogger;
import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.ConnectivityHelper;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Class used for sending logs to a remote service.
 */
public class RemoteLogger {
    private static final String VERBOSE_TAG = "VERBOSE";
    private static final String DEBUG_TAG = "DEBUG";
    private static final String INFO_TAG = "INFO";
    private static final String WARNING_TAG = "WARNING";
    private static final String EXCEPTION_TAG = "EXCEPTION";

    private String TAG;

    private Context mContext;
    private AndroidLogger logEntryLogger = null;
    private AndroidLogger whiteLabeledLogger = null;

    private String mIdentifier;
    private boolean mRemoteLoggingEnabled;
    private boolean mLogToConsole = false;

    public RemoteLogger(Context context, Class thisClass) {
        this(context, thisClass, false);
    }

    public RemoteLogger(Context context, Class thisClass, int logToConsole) {
        this(context, thisClass, false);

        mLogToConsole = logToConsole == 1;
    }
    public RemoteLogger(Context context, Class thisClass, boolean forced) {
        mContext = context;
        createLogger();
        mIdentifier = new Preferences(mContext).getLoggerIdentifier();

        TAG = thisClass.getSimpleName();
        if (forced) {
            forceRemoteLogging(true);
        } else {
            mRemoteLoggingEnabled = new Preferences(mContext).remoteLoggingIsActive();
        }
    }

    public void forceRemoteLogging(boolean forced) {
        mRemoteLoggingEnabled = forced;
    }

    /**
     * Function to create the remote logger instance.
     */
    private void createLogger() {
        File logFile = new File(mContext.getFilesDir(), "LogentriesLogStorage.log");
        if(!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String logEntryToken = mContext.getString(R.string.log_entry_token);
        String secondaryLogEntryToken = mContext.getString(R.string.secondary_log_entry_token);
        // Try to get a existing instance.
        try {
            logEntryLogger =  AndroidLogger.getInstance();
        } catch (Exception e) {
            // Create instance.
            try {
                logEntryLogger = AndroidLogger.createInstance(mContext, false, false, false, null, 0, logEntryToken, false);
                if (!secondaryLogEntryToken.isEmpty()) {
                    whiteLabeledLogger = AndroidLogger.createInstance(mContext, false, false, false, null, 0, secondaryLogEntryToken, false);
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    /**
     * Function to generate a small id used for loggin.
     * @return
     */
    public static String generateIdentifier() {
        String uuid = UUID.randomUUID().toString();
        int stripIndex = uuid.indexOf("-");
        uuid = uuid.substring(0, stripIndex);
        return uuid;
    }

    /**
     * Function to format a message to include severity level and identifier.
     * @param tag Tag that indicates the severity.
     * @param message
     * @return
     */
    private String formatMessage(String tag, String message) {
        return tag + " " + mIdentifier + " - " + getAppVersion() + " - " + getDeviceName()  + " - " + getConnectionType() + " - " + message;
    }

    private String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private String getDeviceName() {
        return Build.BRAND + " " + Build.PRODUCT + " " + "(" + Build.MODEL + ")";
    }

    private String getConnectionType() {
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(mContext);
        if (connectivityHelper.getConnectionType() == ConnectivityHelper.Connection.WIFI || connectivityHelper.getConnectionType() == ConnectivityHelper.Connection.NO_CONNECTION) {
            return connectivityHelper.getConnectionTypeString();
        } else {
            TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();
            return connectivityHelper.getConnectionTypeString() + " (" + carrierName + ")";
        }
    }
    /**
     * Function to log the message for the given tag.
     * @param tag
     * @param message
     */
    private void log(String tag, String message) {
        // Only do remote logging when it is enabled.
        if (mRemoteLoggingEnabled) {
            try {
                if (logEntryLogger != null) {

                    if (TAG.equals(SipService.class.getSimpleName())) {
                        message = anonymizeSipLogging(message);
                    }

                    if (TAG.equals(FcmMessagingService.class.getSimpleName())) {
                        message = anonymizePayloadLogging(message);
                    }

                    if (message.contains("\n")) {
                        message = message.replaceAll("[\r\n]+", " ");
                    }

                    logEntryLogger.log(formatMessage(tag, message));
                    if (whiteLabeledLogger != null) {
                        whiteLabeledLogger.log(formatMessage(tag, message));
                    }
                }
            } catch (Exception e) {
                // Avoid crashing the app in background logging.
            }
        }
    }

    private String anonymizeSipLogging(String message) {
        message = Pattern.compile("sip:\\+?\\d+").matcher(message).replaceAll("sip:SIP_USER_ID");
        message = Pattern.compile("\"caller_id\" = (.+?);").matcher(message).replaceAll("<CALLER_ID>");
        message = Pattern.compile("To:(.+?)>").matcher(message).replaceAll("To: <SIP_ANONYMIZED>");
        message = Pattern.compile("From:(.+?)>").matcher(message).replaceAll("From: <SIP_ANONYMIZED>");
        message = Pattern.compile("Contact:(.+?)>").matcher(message).replaceAll("Contact: <SIP_ANONYMIZED>");
        message = Pattern.compile("Digest username=\"(.+?)\"").matcher(message).replaceAll("Digest username=\"<SIP_USERNAME>\"");
        message = Pattern.compile("nonce=\"(.+?)\"").matcher(message).replaceAll("nonce=\"<NONCE>\"");
        message = Pattern.compile("username=(.+?)&").matcher(message).replaceAll("username=<USERNAME>");

        return message;
    }

    private String anonymizePayloadLogging(String message) {
        message = Pattern.compile("caller_id=(.+?),").matcher(message).replaceAll("callerid=<CALLER_ID>,");
        message = Pattern.compile("phonenumber=(.+?),").matcher(message).replaceAll("phonenumber=<PHONENUMBER>,");

        return message;
    }

    /**
     * Verbose log.
     * @param message
     */
    public void v(String message) {
        log(VERBOSE_TAG, TAG + " " + message);
        if (mLogToConsole) {
            Log.v(TAG, message);
        }
    }

    /**
     * Debug log.
     * @param message
     */
    public void d(String message) {
        log(DEBUG_TAG, TAG + " " + message);
        if (mLogToConsole) {
            Log.d(TAG, message);
        }
    }

    /**
     * Info log.
     * @param message
     */
    public void i(String message) {
        log(INFO_TAG, TAG + " " + message);
        if (mLogToConsole) {
            Log.i(TAG, message);
        }
    }

    /**
     * Warning log.
     * @param message
     */
    public void w(String message) {
        log(WARNING_TAG, TAG + " " + message);
        if (mLogToConsole) {
            Log.w(TAG, message);
        }
    }

    /**
     * Exception log.
     * @param message
     */
    public void e(String message) {
        log(EXCEPTION_TAG, TAG + " " + message);
        if (mLogToConsole) {
            Log.e(TAG, message);
        }
    }

}

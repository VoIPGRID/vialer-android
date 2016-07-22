package com.voipgrid.vialer.util;

import android.content.Context;

import com.logentries.logger.AndroidLogger;
import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Class used for sending logs to a remote service.
 */
public class RemoteLogger {
    private static final String VERBOSE_TAG = "VERBOSE";
    private static final String DEBUG_TAG = "DEBUG";
    private static final String INFO_TAG = "INFO";
    private static final String WARNING_TAG = "WARNING";
    private static final String EXCEPTION_TAG = "EXCEPTION";

    private Context mContext;
    private AndroidLogger logEntryLogger = null;

    private String mIdentifier;
    private boolean mRemoteLoggingEnabled;

    public RemoteLogger(Context context) {
        mContext = context;
        createLogger();
        mIdentifier = new Preferences(mContext).getLoggerIdentifier();
        mRemoteLoggingEnabled = new Preferences(mContext).remoteLoggingIsActive();
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

        // Try to get a existing instance.
        try {
            logEntryLogger =  AndroidLogger.getInstance();
        } catch (Exception e) {
            // Create instance.
            try {
                logEntryLogger = AndroidLogger.createInstance(mContext, false, false, false, null, 0, logEntryToken, false);
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
        return tag + " " + mIdentifier + " - " + message;
    }

    /**
     * Function to log the message for the given tag.
     * @param tag
     * @param message
     */
    private void log(String tag, String message) {
        // Only do remote logging when it is enabled and not a debug build.
        if (mRemoteLoggingEnabled && !BuildConfig.DEBUG) {
            try {
                if (logEntryLogger != null) {
                    logEntryLogger.log(formatMessage(tag, message));
                }
            } catch (Exception e) {
                // Avoid crashing the app in background logging.
            }
        }
    }

    /**
     * Verbose log.
     * @param message
     */
    public void v(String message) {
        log(VERBOSE_TAG, message);
    }

    /**
     * Debug log.
     * @param message
     */
    public void d(String message) {
        log(DEBUG_TAG, message);
    }

    /**
     * Info log.
     * @param message
     */
    public void i(String message) {
        log(INFO_TAG, message);
    }

    /**
     * Warning log.
     * @param message
     */
    public void w(String message) {
        log(WARNING_TAG, message);
    }

    /**
     * Exception log.
     * @param message
     */
    public void e(String message) {
        log(EXCEPTION_TAG, message);
    }

}

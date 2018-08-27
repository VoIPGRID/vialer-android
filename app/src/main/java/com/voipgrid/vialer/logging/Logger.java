package com.voipgrid.vialer.logging;

import android.content.Context;
import android.support.annotation.StringDef;
import android.util.Log;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.file.LogFileCreator;
import com.voipgrid.vialer.logging.formatting.LogFormatter;
import com.voipgrid.vialer.logging.tracing.CallerLocator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class used for sending logs to a remote service.
 */
public class Logger {

    @Retention(RetentionPolicy.CLASS)
    @StringDef({VERBOSE_TAG, DEBUG_TAG, INFO_TAG, WARNING_TAG, EXCEPTION_TAG})
    @interface LogLevels {
    }

    static final String VERBOSE_TAG = "VERBOSE";
    static final String DEBUG_TAG = "DEBUG";
    static final String INFO_TAG = "INFO";
    static final String WARNING_TAG = "WARNING";
    static final String EXCEPTION_TAG = "EXCEPTION";

    private final String tag;

    private Context mContext;
    private static VialerLogger logEntryLogger = null;
    private LogFormatter mLogFormatter;
    private LogComposer mLogComposer;
    private CallerLocator mCallerLocator;
    private LogFileCreator mLogFileCreator;

    private boolean mRemoteLoggingEnabled;
    private boolean mLogToConsole = true;

    public Logger(Class thisClass) {
        mContext = VialerApplication.get();
        mLogFileCreator = new LogFileCreator(mContext);
        createLogger();
        mLogFormatter = new LogFormatter();
        mLogComposer = new LogComposer(
                new DeviceInformation(mContext),
                new Preferences(mContext).getLoggerIdentifier(),
                VialerApplication.getAppVersion()
        );
        mCallerLocator = new CallerLocator();
        mRemoteLoggingEnabled = new Preferences(mContext).remoteLoggingIsActive();
        tag = thisClass.getSimpleName();
    }

    public Logger(Object object) {
        this(object.getClass());
    }

    /**
     * Also logs all messages to the console.
     *
     * @return this To allow method chaining.
     */
    public Logger disableConsoleLogging() {
        mLogToConsole = false;

        return this;
    }

    /**
     * Forces messages to be logged to remote rather than taking the user setting.
     *
     * @return this To allow method chaining.
     */
    public Logger forceRemoteLogging(boolean forced) {
        mRemoteLoggingEnabled = forced;

        return this;
    }

    /**
     *
     * @return VialerLogger The vialer logger being used currently
     */
    public VialerLogger getVialerLogger() {
        return logEntryLogger;
    }

    /**
     * Function to create the remote logger instance.
     */
    private void createLogger() {
        mLogFileCreator.createIfDoesNotExist();

        if(logEntryLogger != null) return;

        logEntryLogger = new VialerLogger(mContext, new LogEntriesFactory()).initialize(new String[] {
                mContext.getString(R.string.log_entry_token),
                mContext.getString(R.string.secondary_log_entry_token)
        });
    }

    /**
     * Function to log the message for the given tag.
     */
    private void log(@LogLevels String level, String message) {
        String tag = createLogTag();

        if (mLogToConsole) {
            logToConsole(level, tag, message);
        }

        if (mRemoteLoggingEnabled) {
            logToRemote(level, tag, message);
        }
    }

    /**
     * Logs the message to the console (logcat).
     */
    private void logToConsole(@LogLevels String level, String tag, String message) {
        switch (level) {
            case VERBOSE_TAG:
                Log.v(tag, message);
                break;
            case DEBUG_TAG:
                Log.d(tag, message);
                break;
            case INFO_TAG:
                Log.i(tag, message);
                break;
            case WARNING_TAG:
                Log.w(tag, message);
                break;
            case EXCEPTION_TAG:
                Log.e(tag, message);
                break;
        }
    }

    /**
     * Logs the message to the remote logger.
     *
     * @param tag
     * @param message
     */
    private void logToRemote(String level, String tag, String message) {
        if( logEntryLogger == null ) return;

        try {
            message = mLogFormatter.applyAllFormatters(tag, message);
            message = mLogComposer.compose(level, tag, message);

            logEntryLogger.log(message);
        } catch (Exception e) {
            // Avoid crashing the app in background logging.
        }
    }

    /**
     * Picks the correct log tag to use, only using the stack trace option when remote logging is enabled for
     * performance reasons.
     *
     * @return String The log tag.
     */
    private String createLogTag() {
        return mRemoteLoggingEnabled ? mCallerLocator.locate().format() : tag;
    }

    /**
     * Verbose log.
     */
    public void v(String message) {
        log(VERBOSE_TAG, message);
    }

    /**
     * Debug log.
     */
    public void d(String message) {
        log(DEBUG_TAG, message);
    }

    /**
     * Info log.
     */
    public void i(String message) {
        log(INFO_TAG, message);
    }

    /**
     * Warning log.
     */
    public void w(String message) {
        log(WARNING_TAG, message);
    }

    /**
     * Exception log.
     */
    public void e(String message) {
        log(EXCEPTION_TAG, message);
    }

}

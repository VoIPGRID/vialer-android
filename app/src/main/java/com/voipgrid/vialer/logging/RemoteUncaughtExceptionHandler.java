package com.voipgrid.vialer.logging;

import android.os.Build;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;

/**
 * Class that sends the uncaught exceptions to remote an presents the regular crash screen
 * afterwards.
 */
public class RemoteUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    RemoteUncaughtExceptionHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public RemoteUncaughtExceptionHandler(RemoteLoggingActivity remoteLoggingActivity) {

    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logStackTrace(throwable);
        mDefaultHandler.uncaughtException(thread, throwable);
    }

    private void logStackTrace(Throwable exception) {
        Logger logger = new Logger(RemoteUncaughtExceptionHandler.class).forceRemoteLogging(true);
        logger.logBufferToRemote();
        String stackTrace = Log.getStackTraceString(exception);
        String traceID = LogUuidGenerator.generate();

        logger.e("*************************************");
        logger.e("************ BEGIN CRASH ************");
        logger.e("************ APP INFO ************");
        logger.e("Version number: " + BuildConfig.VERSION_CODE);
        logger.e("Version name: " + BuildConfig.VERSION_NAME);
        logger.e("************ DEVICE INFORMATION ***********");
        logger.e("Brand: " + Build.BRAND);
        logger.e("Device: " + Build.DEVICE);
        logger.e("Model: " + Build.MODEL);
        logger.e("Id: " + Build.ID);
        logger.e("Product: " + Build.PRODUCT);
        logger.e("************ BUILD INFO ************");
        logger.e("SDK: " + Build.VERSION.SDK_INT);
        logger.e("Release: " + Build.VERSION.RELEASE);
        logger.e("Incremental: " + Build.VERSION.INCREMENTAL);

        logger.e("************ CAUSE OF ERROR ************");
        String[] lines = stackTrace.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logger.e(traceID + "> " + line);
        }
        logger.e("************ END CRASH **************");
        logger.e("*************************************");
    }
}
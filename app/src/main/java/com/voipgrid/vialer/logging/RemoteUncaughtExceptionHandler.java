package com.voipgrid.vialer.logging;

import android.content.Context;
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

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logStackTrace(throwable);
        mDefaultHandler.uncaughtException(thread, throwable);
    }

    private void logStackTrace(Throwable exception) {
        RemoteLogger remoteLogger = new RemoteLogger(RemoteUncaughtExceptionHandler.class).forceRemoteLogging(true);
        String stackTrace = Log.getStackTraceString(exception);
        String traceID = LogUuidGenerator.generate();

        remoteLogger.e("*************************************");
        remoteLogger.e("************ BEGIN CRASH ************");
        remoteLogger.e("************ APP INFO ************");
        remoteLogger.e("Version number: " + BuildConfig.VERSION_CODE);
        remoteLogger.e("Version name: " + BuildConfig.VERSION_NAME);
        remoteLogger.e("************ DEVICE INFORMATION ***********");
        remoteLogger.e("Brand: " + Build.BRAND);
        remoteLogger.e("Device: " + Build.DEVICE);
        remoteLogger.e("Model: " + Build.MODEL);
        remoteLogger.e("Id: " + Build.ID);
        remoteLogger.e("Product: " + Build.PRODUCT);
        remoteLogger.e("************ BUILD INFO ************");
        remoteLogger.e("SDK: " + Build.VERSION.SDK_INT);
        remoteLogger.e("Release: " + Build.VERSION.RELEASE);
        remoteLogger.e("Incremental: " + Build.VERSION.INCREMENTAL);

        remoteLogger.e("************ CAUSE OF ERROR ************");
        String[] lines = stackTrace.split(System.getProperty("line.separator"));
        for (String line : lines) {
            remoteLogger.e(traceID + "> " + line);
        }
        remoteLogger.e("************ END CRASH **************");
        remoteLogger.e("*************************************");
    }
}
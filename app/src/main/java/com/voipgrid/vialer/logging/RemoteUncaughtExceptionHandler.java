package com.voipgrid.vialer.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * Class that sends the uncaught exceptions to remote an presents the regular crash screen
 * afterwards.
 */
public class RemoteUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public RemoteUncaughtExceptionHandler(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logStackTrace(Log.getStackTraceString(throwable));
        mDefaultHandler.uncaughtException(thread, throwable);
    }

    private void logStackTrace(String exception) {
        PackageManager manager = mContext.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo (mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
        }

        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER)) {
            model = Build.MANUFACTURER + " " + model;
        }

        RemoteLogger remoteLogger = new RemoteLogger(mContext, true);
        // Log device information before sending the crash log.
        remoteLogger.e("Android version: " +  Build.VERSION.SDK_INT + "\n");
        remoteLogger.e("Device: " + model + "\n");
        remoteLogger.e("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");
        String[] lines = exception.split(System.getProperty("line.separator"));
        for (int i = 0; i < lines.length; i++) {
            remoteLogger.e(lines[i]);
        }
    }
}

package com.voipgrid.vialer.util;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;

import java.io.IOException;
import java.util.Scanner;

public class StackTraceLog extends MainActivity implements View.OnClickListener {
    private RemoteLogger mRemoteLogger;
    private ConnectivityHelper mConnectivityHelper;
    private final static String TAG = StackTraceLog.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        mRemoteLogger = new RemoteLogger(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setFinishOnTouchOutside(false); // prevent users from dismissing the dialog by tapping outside
        setContentView(R.layout.stack_trace_log);
        showDialog();
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.remote_logging_unexpected_error));
        builder.setMessage(getString(R.string.remote_logging_send_crash_log));
        builder.setPositiveButton(getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sendLog();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendLog() {
        ConnectivityHelper mConnectivityHelper = ConnectivityHelper.get(this);
        if (!mConnectivityHelper.hasNetworkConnection()) {
            Toast.makeText(this, getString(R.string.onboarding_no_internet_message), Toast.LENGTH_SHORT).show();
            showDialog();
        } else {
            logStackTrace();
        }
    }

    private void logStackTrace() {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo (this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
        }

        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

            String cmd = "logcat -d -v time";

            // Get input stream
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                e.printStackTrace();
                mRemoteLogger.d(TAG + " StackTraceLogging Failed");
                return;
            }

            // Log device information before sending the crash log.
            mRemoteLogger.d("Android version: " +  Build.VERSION.SDK_INT + "\n");
            mRemoteLogger.d("Device: " + model + "\n");
            mRemoteLogger.d("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");

            String stacktrace = convertStreamToString(process.getInputStream());
            // We only want to log the exception, therefor we split the log.
            // Since the exception caused this code to run it will be on top of the log.
            // By splitting on 'beginning' we will get the beginning of crash.
            String exceptionTrace = stacktrace.split("(?=beginning)")[1];

            mRemoteLogger.d(exceptionTrace);

            // Making sure the app gets killed.
            System.exit(1);
    }

    public static String convertStreamToString(java.io.InputStream is) {
        Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}

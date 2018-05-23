package com.voipgrid.vialer.logging;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity that implements our own send logs to developer option.
 */
public class RemoteLoggingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new RemoteUncaughtExceptionHandler(this));
    }

    /**
     * Check if all permissions are granted
     *
     * @see android.app.Activity#onRequestPermissionsResult(int, String[], int[])
     * @param permissions
     * @param grantResults
     * @return
     */
    public boolean allPermissionsGranted(String[] permissions, int[] grantResults) {
        boolean allPermissionsGranted = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        return allPermissionsGranted;
    }
}

package com.voipgrid.vialer.logging;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that implements our own send logs to developer option.
 */
public class RemoteLoggingActivity extends AppCompatActivity {

    protected Logger mLogger;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new RemoteUncaughtExceptionHandler());

        mLogger = new Logger(this.getClass());
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

    /**
     * Check whether the user can currently interact with the screen.
     *
     * @return TRUE if the user can interact with the screen, otherwise FALSE
     */
    protected boolean isScreenInteractive() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return powerManager.isScreenOn();
        } else {
            return powerManager.isInteractive();
        }
    }
}

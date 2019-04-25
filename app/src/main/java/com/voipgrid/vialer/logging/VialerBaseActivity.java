package com.voipgrid.vialer.logging;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ConnectivityHelper;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that implements our own send logs to developer option.
 */
public abstract class VialerBaseActivity extends AppCompatActivity {

    protected Logger mLogger;

    @Inject protected BroadcastReceiverManager broadcastReceiverManager;
    @Inject protected ConnectivityHelper connectivityHelper;
    @Inject protected ConnectivityManager connectivityManager;

    private NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();
    private NetworkCallback networkCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new RemoteUncaughtExceptionHandler());
        mLogger = new Logger(this);
        VialerApplication.get().component().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback = new NetworkCallback());
        } else {
            broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(networkChangeReceiver, CONNECTIVITY_ACTION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } else {
            broadcastReceiverManager.unregisterReceiver(networkChangeReceiver);
        }
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

    /**
     * Called when internet connectivity is lost, this will also send when it is a sticky broadcast
     * so when the activity is first loaded.
     *
     */
    protected void onInternetConnectivityLost() {
    }

    /**
     * Called when internet connectivity is gained, this will also send when it is a sticky broadcast
     * so when the activity is first loaded.
     *
     */
    protected void onInternetConnectivityGained() {
    }

    /**
     * Check the current status of internet connectivity.
     *
     * @return
     */
    protected boolean hasInternetConnectivity() {
        return connectivityHelper.hasNetworkConnection();
    }

    /**
     * This is a network callback for the current recommended method of detecting
     * network changes.
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            runOnUiThread(VialerBaseActivity.this::onInternetConnectivityGained);
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            runOnUiThread(VialerBaseActivity.this::onInternetConnectivityLost);
        }
    }

    /**
     * This is an out-dated method of detecting network changes and should be
     * removed when we no longer need to support these versions.
     *
     */
    private class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(EXTRA_NO_CONNECTIVITY, false)) {
                onInternetConnectivityLost();
            } else {
                onInternetConnectivityGained();
            }
        }
    }
}

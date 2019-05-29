package com.voipgrid.vialer.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.statistics.AppState;

import java.util.List;

public class AudioStateChangeReceiver extends BroadcastReceiver {

    private static BluetoothProfile sBluetoothProfile;
    private static BluetoothProfile.ServiceListener sBluetoothListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            List<BluetoothDevice> devices = bluetoothProfile.getConnectedDevices();

            if (devices.size() > 0) {
                AppState.lastConnectedBluetoothHeadsetName = devices.get(0).getName();
            }

            sBluetoothProfile = bluetoothProfile;
        }

        @Override
        public void onServiceDisconnected(int i) {

        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        fetch();
    }

    /**
     * Asynchronously fetches the currently connected bluetooth headset and saves it as a static
     * class property.
     */
    public static void fetch() {
        new Thread(() -> {
            BluetoothManager bluetoothManager = (BluetoothManager) VialerApplication.get().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return;
            }
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

            if (bluetoothAdapter == null) {
                return;
            }

            if (sBluetoothProfile != null) {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, sBluetoothProfile);
            }
            bluetoothAdapter.getProfileProxy(VialerApplication.get(),sBluetoothListener, BluetoothProfile.HEADSET);
        }).start();
    }
}

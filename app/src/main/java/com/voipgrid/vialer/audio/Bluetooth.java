package com.voipgrid.vialer.audio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;

import com.voipgrid.vialer.logging.Logger;

class Bluetooth {

    private final Logger logger;
    private final AudioManager audioManager;

    Bluetooth(AudioManager audioManager) {
        this.audioManager = audioManager;
        this.logger = new Logger(this);
    }

    /**
     * Check if there is a communication device currently connected that we could route
     * audio through.
     *
     * @return TRUE if there is a bluetooth communication device present, not necessarily if audio is being routed
     * over bluetooth otherwise FALSE.
     */
    boolean isBluetoothCommunicationDevicePresent() {
        logger.v("hasBluetoothHeadset()");

        BluetoothAdapter bluetoothAdapter;
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            logger.e("BluetoothAdapter.getDefaultAdapter() exception: " + e.getMessage());
            return false;
        }

        if (bluetoothAdapter == null) {
            logger.e("There is no bluetoothAdapter!?");
            return false;
        }

        int profileConnectionState;
        try {
            profileConnectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        } catch (Exception e) {
            logger.e("BluetoothAdapter.getProfileConnectionState() exception: " + e.getMessage());
            profileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        }
        logger.i("Bluetooth profile connection state: " + profileConnectionState);
        return bluetoothAdapter.isEnabled() && profileConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    boolean isOn() {
        return audioManager.isBluetoothScoOn();
    }

    void start() {
        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
    }

    void stop() {
        logger.v("stopBluetoothSco()");

        if (!audioManager.isBluetoothScoOn()) {
            logger.i("==> Unable to stop Bluetooth sco since it is already disabled!");
            return;
        }

        logger.d("==> turning Bluetooth sco off");
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);

        int retries = 0;
        while(audioManager.isBluetoothScoOn() && retries < 10) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);

            logger.i("Retry of stopping bluetooth sco: " + retries);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}

            if (!audioManager.isBluetoothScoOn()) {
                return;
            }

            retries++;
        }
    }

}

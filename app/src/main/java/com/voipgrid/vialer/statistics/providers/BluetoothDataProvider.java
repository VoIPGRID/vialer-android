package com.voipgrid.vialer.statistics.providers;

import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_BLUETOOTH_AUDIO_ENABLED_FALSE;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_BLUETOOTH_AUDIO_ENABLED_TRUE;

import com.voipgrid.vialer.statistics.AppState;

public class BluetoothDataProvider {

    public String getBluetoothAudioEnabled() {
        return AppState.isUsingBluetoothAudio ? VALUE_BLUETOOTH_AUDIO_ENABLED_TRUE : VALUE_BLUETOOTH_AUDIO_ENABLED_FALSE;
    }

    public String getBluetoothDeviceName() {
        return AppState.lastConnectedBluetoothHeadsetName;
    }
}

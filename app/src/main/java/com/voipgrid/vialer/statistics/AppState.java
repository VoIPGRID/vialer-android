package com.voipgrid.vialer.statistics;

/**
 * Contains various flags that indicate the current state of the app.
 */
public final class AppState {

    private AppState() {}

    /**
     * Set to TRUE if bluetooth audio is currently being used.
     */
    public static boolean isUsingBluetoothAudio = false;

    /**
     * Set to the name of the last bluetooth headset that was connected.
     */
    public static String lastConnectedBluetoothHeadsetName;
}

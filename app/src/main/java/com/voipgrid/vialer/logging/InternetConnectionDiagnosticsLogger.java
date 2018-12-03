package com.voipgrid.vialer.logging;

import com.voipgrid.vialer.VialerApplication;

import java.util.HashMap;

import github.nisrulz.easydeviceinfo.base.EasyNetworkMod;
import github.nisrulz.easydeviceinfo.base.NetworkType;

public class InternetConnectionDiagnosticsLogger {

    private final Logger mLogger = new Logger(this);
    private final EasyNetworkMod mEasyNetworkMod;

    private static final HashMap<Integer, String> networkTypeMap = new HashMap<>();

    static {
        networkTypeMap.put(NetworkType.CELLULAR_UNKNOWN, "No internet connection");
        networkTypeMap.put(NetworkType.CELLULAR_UNIDENTIFIED_GEN, "Cellular Unidentified Generation");
        networkTypeMap.put(NetworkType.CELLULAR_2G, "Cellular 2G");
        networkTypeMap.put(NetworkType.CELLULAR_3G, "Cellular 3G");
        networkTypeMap.put(NetworkType.CELLULAR_4G, "Cellular 4G");
        networkTypeMap.put(NetworkType.WIFI_WIFIMAX, "WiFi");
        networkTypeMap.put(NetworkType.UNKNOWN, networkTypeMap.get(NetworkType.CELLULAR_UNKNOWN));
    }

    public InternetConnectionDiagnosticsLogger() {
        mEasyNetworkMod = new EasyNetworkMod(VialerApplication.get());
    }


    /**
     * Log the internet diagnostics message.
     *
     */
    public void log() {
        mLogger.i(create());
    }

    /**
     * Prepare a string for logging purposes.
     *
     * @return
     */
    public String create() {
        if (!mEasyNetworkMod.isNetworkAvailable()) {
            return "No internet connection is available";
        }

        return "Internet connection is available, type: " + getNetworkTypeString();
    }

    private String getNetworkTypeString() {
        String networkType =  networkTypeMap.get(mEasyNetworkMod.getNetworkType());

        if (networkType == null) {
            return networkTypeMap.get(NetworkType.UNKNOWN);
        }

        return networkType;
    }
}

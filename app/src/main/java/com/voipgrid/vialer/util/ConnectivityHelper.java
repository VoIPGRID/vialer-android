package com.voipgrid.vialer.util;

import static android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO;
import static android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA;
import static android.telephony.TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to check connectivity of the device.
 */
public class ConnectivityHelper {
    public enum Connection {
        NO_CONNECTION("Unknown", -1),
        SLOW("Slow", 0),
        WIFI("Wifi", 1),
        LTE("4G", 2),
        HSDPA("HSDPA", 3),     // ~ 2-14 Mbps
        HSPAP("HSPAP", 4),     // ~ 10-20 Mbps
        HSUPA("HSUPA", 5),     // ~ 1-23 Mbps
        EVDO_B("EVDO_B", 6),
        FIVE_G("FIVE_G", 7);

        String stringValue;
        int intValue;

        Connection(String string, int intValue) {
            stringValue = string;
            this.intValue = intValue;
        }

        public int toInt() {
            return intValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    private final ConnectivityManager mConnectivityManager;
    private final TelephonyManager mTelephonyManager;
    public static boolean mWifiKilled = false;

    private static final List<Connection> sFastDataTypes = new ArrayList<>();

    static {
        sFastDataTypes.add(Connection.WIFI);
        sFastDataTypes.add(Connection.LTE);
        sFastDataTypes.add(Connection.FIVE_G);
    }

    /**
     * Constructor.
     * @param connectivityManager
     * @param telephonyManager
     */
    public ConnectivityHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager) {
        mConnectivityManager = connectivityManager;
        mTelephonyManager = telephonyManager;
    }

    /**
     * Check the device current connectivity state based on the active network.
     * @return
     */
    public boolean hasNetworkConnection() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Get the current connection type.
     * @return Long representation of the connection type.
     */
    public Connection getConnectionType() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return Connection.NO_CONNECTION;
        }

        // We need to check 2 methods for the type because they both can give a different
        // value for the same information we are checking.
        // Get network type from ConnectivityManager.
        int networkTypeConnection = info.getSubtype();
        // Get network type from TelephonyManager.
        int networkTypeTelephony = mTelephonyManager.getDataNetworkType();

        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            return Connection.WIFI;
        }
        else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_NR) {
            return Connection.FIVE_G;
        }
        else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_LTE || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_LTE) {
            return Connection.LTE;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSDPA || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSDPA) {
            return Connection.HSDPA;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSPAP || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSPAP) {
            return Connection.HSPAP;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSUPA || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSUPA) {
            return Connection.HSUPA;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_EVDO_B || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_EVDO_B) {
            return Connection.EVDO_B;
        }
        return Connection.SLOW;
    }

    /**
     * Get the connection type as string. This is mainly used for the GA tracking.
     *
     * @return String representation of the connection type.
     */
    public String getConnectionTypeString() {
        String connectionString;

        switch (getConnectionType()) {
            case WIFI:
                connectionString = Connection.WIFI.toString();
                break;
            case FIVE_G:
                connectionString = Connection.FIVE_G.toString();
                break;
            case LTE:
                connectionString = Connection.LTE.toString();
                break;
            case HSDPA:
                connectionString = Connection.HSDPA.toString();
                break;
            case HSPAP:
                connectionString = Connection.HSPAP.toString();
                break;
            case HSUPA:
                connectionString = Connection.HSUPA.toString();
                break;
            case EVDO_B:
                connectionString = Connection.EVDO_B.toString();
                break;
            default:
                connectionString = Connection.NO_CONNECTION.toString();
                break;
        }

        return connectionString;
    }

    /**
     * Check if the device is connected via wifi or LTE connection.
     * @return
     */
    public boolean hasFastData() {
        return sFastDataTypes.contains(getConnectionType());
    }

    public static ConnectivityHelper get(Context context) {
        ConnectivityManager c = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager t = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return new ConnectivityHelper(c, t);
    }

    public void useWifi(Context context, boolean useWifi) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(useWifi);
    }

    /**
     * Sets a timer that runs until the timeout limit is reached. At each interval it
     * will check whether or not we succeeded in enabling LTE. If it did not succeed then
     * we will try to get Wifi on again.
     */
    private void waitForLTE(final Context context, int timeout, final int interval) {
        final int remainingTime = timeout - interval;
        new Handler().postDelayed(() -> {
            // Keep waiting until the remaining time is less then the interval.
            if(remainingTime > interval) {
                waitForLTE(context, remainingTime, interval);
            } else if(getConnectionType() != Connection.LTE) {
                // Turn wifi back on if we don't succeed in connecting with LTE before the timeout.
                useWifi(context, true);
            }
        }, interval);
    }

    void attemptUsingLTE(final Context context, int timeout) {
        if (getConnectionType() == Connection.WIFI) {
            useWifi(context, false);
            mWifiKilled = true;
            waitForLTE(context, timeout+(timeout/10), timeout/10);
        }
    }
}

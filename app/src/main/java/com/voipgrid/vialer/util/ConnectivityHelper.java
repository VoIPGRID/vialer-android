package com.voipgrid.vialer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to check connectivity of the device.
 */
public class ConnectivityHelper {
    public static final int TYPE_NO_CONNECTION = -1;
    public static final int TYPE_SLOW = 0;
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_LTE = 2;
    public static final int TYPE_HSDPA = 3;
    public static final int TYPE_HSPAP = 4;
    public static final int TYPE_HSUPA = 5;
    public static final int TYPE_EVDO_B = 6;

    public static final String CONNECTION_WIFI = "Wifi";
    public static final String CONNECTION_4G = "4G";
    public static final String CONNECTION_HSDPA = "HSDPA"; // ~ 2-14 Mbps
    public static final String CONNECTION_HSPAP = "HSPAP"; // ~ 10-20 Mbps
    public static final String CONNECTION_HSUPA = "HSUPA"; // ~ 1-23 Mbps
    public static final String CONNECTION_EVDO_B = "EVDO_B"; // ~ 5 Mbps
    public static final String CONNECTION_UNKNOWN = "unknown";

    private final ConnectivityManager mConnectivityManager;
    private final TelephonyManager mTelephonyManager;
    public static boolean mWifiKilled = false;

    private static final List<Integer> sFastDataTypes = new ArrayList<>();
    private static final List<Integer> sFast3GDataTypes = new ArrayList<>();

    private static Context mContext;

    static {
        sFastDataTypes.add(TYPE_WIFI);
        sFastDataTypes.add(TYPE_LTE);
    }

    static {
        sFast3GDataTypes.add(TYPE_HSDPA);
        sFast3GDataTypes.add(TYPE_HSPAP);
        sFast3GDataTypes.add(TYPE_HSUPA);
        sFast3GDataTypes.add(TYPE_EVDO_B);
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
    public int getConnectionType() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return TYPE_NO_CONNECTION;
        }

        // We need to check 2 methods for the type because they both can give a different
        // value for the same information we are checking.
        // Get network type from ConnectivityManager.
        int networkTypeConnection = info.getSubtype();
        // Get network type from TelephonyManager.
        int networkTypeTelephony = mTelephonyManager.getNetworkType();

        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            return TYPE_WIFI;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_LTE || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_LTE) {
            return TYPE_LTE;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSDPA || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSDPA) {
            return TYPE_HSDPA;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSPAP || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSPAP) {
            return TYPE_HSPAP;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSUPA || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSUPA) {
            return TYPE_HSUPA;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_EVDO_B || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_EVDO_B) {
            return TYPE_EVDO_B;
        } else {
            return TYPE_SLOW;
        }
    }

    /**
     * Get the connection type as string. This is mainly used for the GA tracking.
     *
     * @return String representation of the connection type.
     */
    public String getConnectionTypeString() {
        String connectionString;

        switch (getConnectionType()) {
            case TYPE_WIFI:
                connectionString = CONNECTION_WIFI;
                break;
            case TYPE_LTE:
                connectionString = CONNECTION_4G;
                break;
            case TYPE_HSDPA:
                connectionString = CONNECTION_HSDPA;
                break;
            case TYPE_HSPAP:
                connectionString = CONNECTION_HSPAP;
                break;
            case TYPE_HSUPA:
                connectionString = CONNECTION_HSUPA;
                break;
            case TYPE_EVDO_B:
                connectionString = CONNECTION_EVDO_B;
                break;
            default:
                connectionString = CONNECTION_UNKNOWN;
                break;
        }

        return connectionString;
    }

    public String getAnalyticsLabel() {
        String analyticsLabel;
        switch (getConnectionType()) {
            case TYPE_WIFI:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_wifi);
                break;
            case TYPE_LTE:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_4g);
                break;
            case TYPE_HSDPA:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_hsdpa);
                break;
            case TYPE_HSPAP:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_hspap);
                break;
            case TYPE_HSUPA:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_hsupa);
                break;
            case TYPE_EVDO_B:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_evdo_b);
                break;
            default:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_unknown);
                break;
        }
        return analyticsLabel;
    }

    /**
     * Check if the device is connected via wifi or LTE connection.
     * @return
     */
    public boolean hasFastData() {
        Preferences pref = new Preferences(mContext);
        int connectionType = getConnectionType();
        return sFastDataTypes.contains(connectionType) || (sFast3GDataTypes.contains(connectionType) && pref.has3GEnabled());
    }

    public static ConnectivityHelper get(Context context) {
        ConnectivityManager c = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager t = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mContext = context;
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
    public void waitForLTE(final Context context, int timeout, final int interval) {
        final int remainingTime = timeout - interval;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Keep waiting until the remaining time is less then the interval.
                if(remainingTime > interval) {
                    waitForLTE(context, remainingTime, interval);
                } else if(getConnectionType() != TYPE_LTE) {
                    // Turn wifi back on if we don't succeed in connecting with LTE before the timeout.
                    useWifi(context, true);
                }
            }
        }, interval);
    }

    public void attemptUsingLTE(final Context context, int timeout) {
        if (getConnectionType() == ConnectivityManager.TYPE_WIFI) {
            useWifi(context, false);
            mWifiKilled = true;
            waitForLTE(context, timeout+(timeout/10), timeout/10);
        }
    }
}

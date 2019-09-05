package com.voipgrid.vialer.statistics.providers;

import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_APP_STATUS_ALPHA;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_APP_STATUS_BETA;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_APP_STATUS_PRODUCTION;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_NETWORK_3G;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_NETWORK_4G;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_NETWORK_NO_CONNECTION;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_NETWORK_UNKNOWN;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_NETWORK_WIFI;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.util.ConnectivityHelper;

public class DefaultDataProvider {

    public String getLogId() {
        return User.remoteLogging.getId();
    }

    public String getClientCountry() {
        if (!User.getHasPhoneAccount()) {
            return null;
        }

        return User.getPhoneAccount().getCountry();
    }

    public String getNetworkOperator() {
        TelephonyManager manager = (TelephonyManager) VialerApplication.get().getSystemService(
                Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    public String getNetwork() {
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(VialerApplication.get());
        String type = connectivityHelper.getConnectionTypeString();

        if (type == null) {
            return VALUE_NETWORK_UNKNOWN;
        }

        if (type.equalsIgnoreCase(ConnectivityHelper.Connection.WIFI.toString())) {
            return VALUE_NETWORK_WIFI;
        }

        if (type.equalsIgnoreCase(ConnectivityHelper.Connection.LTE.toString())) {
            return VALUE_NETWORK_4G;
        }

        if (type.equalsIgnoreCase(ConnectivityHelper.Connection.NO_CONNECTION.toString())) {
            return VALUE_NETWORK_NO_CONNECTION;
        }

        return VALUE_NETWORK_3G;
    }

    /**
     * Get whether app is alpha, beta or production.
     *
     * @return
     */
    public String getAppStatus() {
        String versionName = BuildConfig.VERSION_NAME;

        if (versionName.contains("alpha")) {
            return VALUE_APP_STATUS_ALPHA;
        }

        if (versionName.contains("beta")) {
            return VALUE_APP_STATUS_BETA;
        }

        return VALUE_APP_STATUS_PRODUCTION;
    }

    public String getOSVersion() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    public String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    public String getDeviceModel() {
        return Build.MODEL;
    }

    public String getAppVersion() {
        return VialerApplication.getAppVersion();
    }

    public String getSipUserId() {
        if (!User.getHasPhoneAccount()) {
            return null;
        }

        return User.getPhoneAccount().getAccountId();
    }
}

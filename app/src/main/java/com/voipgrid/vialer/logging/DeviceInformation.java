package com.voipgrid.vialer.logging;


import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.util.ConnectivityHelper;

public class DeviceInformation {

    private Context mContext;

    public DeviceInformation(Context context) {
        mContext = context;
    }

    public String getDeviceName() {
        return Build.BRAND + " " + Build.PRODUCT + " " + "(" + Build.MODEL + ")";
    }

    public String getConnectionType() {
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(mContext);
        if (connectivityHelper.getConnectionType() == ConnectivityHelper.Connection.WIFI || connectivityHelper.getConnectionType() == ConnectivityHelper.Connection.NO_CONNECTION) {
            return connectivityHelper.getConnectionTypeString();
        } else {
            TelephonyManager manager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();
            return connectivityHelper.getConnectionTypeString() + " (" + carrierName + ")";
        }
    }
}

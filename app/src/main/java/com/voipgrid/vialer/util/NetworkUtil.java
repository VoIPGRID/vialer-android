package com.voipgrid.vialer.util;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    private final Context mContext;

    public NetworkUtil (Context context) {
        mContext= context;
    }

    /**
     * @see <a href="https://stackoverflow.com/questions/15698790/broadcast-receiver-for-checking-internet-connection-in-android-app/15700728#15700728">stackoverflow article</a>
     */

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }
}

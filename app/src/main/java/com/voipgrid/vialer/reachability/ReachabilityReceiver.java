package com.voipgrid.vialer.reachability;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.voipgrid.vialer.logging.Logger;


public class ReachabilityReceiver extends BroadcastReceiver {

    private Context mContext;
    private static ReachabilityInterface mReachabilityInterface;
    private Logger mLogger;

    public ReachabilityReceiver(Context context) {
        mContext = context;
        mLogger = new Logger(ReachabilityReceiver.class);
    }

    public void startListening() {
        mContext.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (mReachabilityInterface != null) {
            mReachabilityInterface.networkChange();
        }
    }

    public void stopListening() {
        try {
            mContext.unregisterReceiver(this);
        } catch(IllegalArgumentException e) {
            mLogger.w("Trying to unregister ConnectivityManager.CONNECTIVITY_ACTION not registered.");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mReachabilityInterface != null) {
            mReachabilityInterface.networkChange();
        }
    }

    static void setInterfaceCallback(ReachabilityInterface listener) {
        mReachabilityInterface = listener;
    }
}

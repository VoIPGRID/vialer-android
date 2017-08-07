package com.voipgrid.vialer.reachability;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;


public class ReachabilityReceiver extends BroadcastReceiver {

    private Context mContext;
    private static ReachabilityInterface mReachabilityInterface;

    public ReachabilityReceiver(Context context) {
        mContext = context;
    }

    public void startListening() {
        mContext.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (mReachabilityInterface != null) {
            mReachabilityInterface.networkChange();
        }
    }

    public void stopListening() {
        mContext.unregisterReceiver(this);
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

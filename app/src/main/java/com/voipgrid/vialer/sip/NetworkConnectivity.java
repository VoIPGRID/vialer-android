package com.voipgrid.vialer.sip;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.calling.NetworkAvailabilityActivity;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.util.NetworkUtil;

import javax.inject.Inject;


public class NetworkConnectivity extends BroadcastReceiver {

    @Inject NetworkUtil mNetworkUtil;
    NetworkAvailabilityActivity mNetworkAvailabilityActivity = new NetworkAvailabilityActivity();

    private final Logger mLogger;


    public NetworkConnectivity() {
        mLogger = new Logger(this.getClass());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isInitialStickyBroadcast()) {
            mLogger.i("Ignoring network change as broadcast is old (sticky).");
            return;
        }

        VialerApplication.get().component().inject(this);

        if(!mNetworkUtil.isOnline()) {
            mNetworkAvailabilityActivity.start();
        }
    }
}

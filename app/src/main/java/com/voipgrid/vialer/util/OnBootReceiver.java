package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.middleware.Middleware;

import javax.inject.Inject;


public class OnBootReceiver extends BroadcastReceiver {

    @Inject Middleware middleware;

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger logger = new Logger(OnBootReceiver.class);
        logger.e("onBootReceiver");
        VialerApplication.get().component().inject(this);
        middleware.register();
    }
}

package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;


public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger logger = new Logger(OnBootReceiver.class);
        logger.e("onBootReceiver");

        MiddlewareHelper.registerAtMiddleware(context);
    }
}

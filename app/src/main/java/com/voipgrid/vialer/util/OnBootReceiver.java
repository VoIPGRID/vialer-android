package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;


public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RemoteLogger remoteLogger = new RemoteLogger(OnBootReceiver.class).enableConsoleLogging();
        remoteLogger.e("onBootReceiver");

        MiddlewareHelper.registerAtMiddleware(context);
    }
}

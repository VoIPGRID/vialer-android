package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.fcm.FcmRegistrationService;


public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intents = new Intent(context, FcmRegistrationService.class);
        context.startService(intents);
    }
}

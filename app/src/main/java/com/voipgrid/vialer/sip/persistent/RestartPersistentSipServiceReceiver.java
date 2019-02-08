package com.voipgrid.vialer.sip.persistent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.voipgrid.vialer.VialerApplication;

public class RestartPersistentSipServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        PersistentSipService.start();

        scheduleAlarm();
    }


    public static void scheduleAlarm() {
        Intent i = new Intent(VialerApplication.get(), RestartPersistentSipServiceReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(VialerApplication.get(), 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) VialerApplication.get().getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (60 * 1000),pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (60 * 1000), pendingIntent);
        }
    }
}

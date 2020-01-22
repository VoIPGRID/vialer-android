package com.voipgrid.vialer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.app.TaskStackBuilder
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.settings.SettingsActivity

class VoipDisabledNotification : AbstractNotification() {

    override val notificationId: Int = 2

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context : Context): NotificationChannel {
        return NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_voip_disabled),
                NotificationManager.IMPORTANCE_HIGH
        );
    }

    override fun buildNotification(context: Context): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentText(context.getString(R.string.notification_channel_voip_disabled))
                .setAutoCancel(true)
                .setPriority(PRIORITY_MAX)
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_channel_voip_disabled)))

        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(Intent(context, SettingsActivity::class.java))

        builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))

        // For SDK version greater than 21 we will set the vibration.
        if (Build.VERSION.SDK_INT >= 21) {
            builder.setDefaults(android.app.Notification.DEFAULT_VIBRATE or android.app.Notification.DEFAULT_LIGHTS)
        }

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID: String = "vialer_voip_disabled"
    }
}
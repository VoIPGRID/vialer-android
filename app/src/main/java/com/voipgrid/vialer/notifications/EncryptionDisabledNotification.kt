package com.voipgrid.vialer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.SettingsActivity

class EncryptionDisabledNotification : AbstractNotification() {

    override val notificationId: Int = 3

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context : Context): NotificationChannel {
        return NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_encryption_disabled),
                NotificationManager.IMPORTANCE_LOW
        )
    }

    override fun buildNotification(context: Context): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lock_open)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(context.getString(R.string.encrypted_calling_notification_title))
                .setContentText(context.getString(R.string.encrypted_calling_notification_description))
                .setOngoing(true)
                .setVibrate(null)

        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(Intent(context, SettingsActivity::class.java))

        builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID: String = "vialer_encryption_disabled"
    }
}
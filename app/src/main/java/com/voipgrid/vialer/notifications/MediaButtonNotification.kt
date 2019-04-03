package com.voipgrid.vialer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R

class MediaButtonNotification : AbstractNotification() {
    override val notificationId = 999

    override fun buildNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_media_button),
                NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationChannel.enableVibration(false)

        return notificationChannel
    }

    companion object {
        const val CHANNEL_ID: String = "vialer_media_button"
    }
}
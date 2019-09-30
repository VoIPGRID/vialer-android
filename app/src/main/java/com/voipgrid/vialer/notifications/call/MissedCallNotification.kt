package com.voipgrid.vialer.notifications.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import java.util.*


class MissedCallNotification(private val number: String) : AbstractCallNotification() {

    override val notificationId = (Date().time / 1000L % Integer.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID: String = "missed_notification"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        return NotificationChannel(
                CHANNEL_ID,
                "Call missed",
                NotificationManager.IMPORTANCE_HIGH
        )
    }

    override fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
    }

    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        val intent = Intent(builder.mContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(builder.mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return builder
                .setOngoing(false)
                .setContentTitle(context.getString(R.string.missed_call, number))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
    }
}
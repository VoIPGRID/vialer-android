package com.voipgrid.vialer.notifications.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R
import java.util.*

class AnsweredElsewhereNotification(private val number: String) : AbstractCallNotification() {

    override val notificationId = (Date().time / 1000L % Integer.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID: String = "answered_elsewhere_notification"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        return NotificationChannel(
                CHANNEL_ID,
                "Answered elsewhere",
                NotificationManager.IMPORTANCE_HIGH
        )
    }

    override fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
    }

    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
                .setOngoing(false)
                .setContentTitle(context.getString(R.string.answered_elsewhere, number))
                .setPriority(NotificationCompat.PRIORITY_MIN)
    }
}
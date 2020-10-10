package com.voipgrid.vialer.notifications.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R.drawable
import com.voipgrid.vialer.R.string
import com.voipgrid.vialer.sip.SipService


class IncomingCallNotification(private val number : String, private val callerId : String) : AbstractCallNotification() {

    override val notificationId = 333

    /**
     * We are creating a new notification channel for incoming calls because they
     * will have a different priority to the other notifications.
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(string.notification_channel_incoming_calls),
                NotificationManager.IMPORTANCE_HIGH
        )

        channel.enableVibration(false)
        channel.setSound(null, null)
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        return channel
    }

    /**
     * Create a custom notification builder as we use a custom channel for this
     * notification.
     *
     */
    override fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
    }

    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder.setSmallIcon(drawable.ic_logo)
                .setContentTitle(createNotificationTitle())
                .setContentText(number)
                .setContentIntent(createIncomingCallActivityPendingIntent())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(createIncomingCallActivityPendingIntent(), true)
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(number))
                .addAction(
                        drawable.ic_call_decline_normal,
                        context.getString(string.call_incoming_decline),
                        SipService.createSipServiceAction(SipService.Actions.DECLINE_INCOMING_CALL)
                )
                .addAction(
                        drawable.ic_call_answer_normal,
                        context.getString(string.call_incoming_accept),
                        SipService.createSipServiceAction(SipService.Actions.ANSWER_INCOMING_CALL)
                )
                .setSound(null)
    }

    private fun createNotificationTitle() : String {
        val display = if (callerId.isEmpty()) number else callerId

        return "${context.getString(string.call_incoming_expanded)} $display"
    }

    companion object {
        const val CHANNEL_ID: String = "vialer_incoming_calls"
    }
}
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


    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder.setSmallIcon(drawable.call_notification_icon)
                .setContentTitle(createNotificationTitle())
                .setContentText(number)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(createCallActivityPendingIntent(), true)
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
}
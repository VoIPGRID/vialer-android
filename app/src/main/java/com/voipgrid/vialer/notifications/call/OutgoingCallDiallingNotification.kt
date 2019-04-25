package com.voipgrid.vialer.notifications.call

import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R

class OutgoingCallDiallingNotification(private val number : String, private val callerId : String?) : AbstractCallNotification() {

    /**
     * Build an outgoing call notification for during dialling.
     *
     */
    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
                .setOngoing(true)
                .setContentTitle(createNotificationTitle())
                .setContentText(context.getString(R.string.callnotification_active_call))
                .setContentIntent(createCallActivityPendingIntent())
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(number))
    }

    private fun createNotificationTitle() : String {
        val dialing = context.getString(R.string.callnotification_dialing)

        if (!callerId.isNullOrEmpty()) {
            return "$dialing $callerId ($number)"
        }

        return "$dialing $number"
    }
}
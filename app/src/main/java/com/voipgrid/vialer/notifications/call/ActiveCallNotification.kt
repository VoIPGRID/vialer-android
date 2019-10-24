package com.voipgrid.vialer.notifications.call

import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R
import nl.voipgrid.vialer_voip.core.call.Call


class ActiveCallNotification(private val call : Call) : AbstractCallNotification() {

    /**
     * Build the active call notification, this includes the call duration so
     * it is expected that this is called every second during an active call..
     *
     */
    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
                .setContentTitle(createNotificationTitle())
                .setContentText(context.getString(R.string.callnotification_active_call))
                .setSubText(call.getDuration(Call.DurationUnit.SECONDS).toString())
                .setShowWhen(false)
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(call.metaData.number))
    }

    /**
     * Generate the notification title, this will change depending on whether
     * there is a call id.
     *
     */
    private fun createNotificationTitle() : String {
        if (call.metaData.callerId.isBlank()) {
            return call.metaData.number
        }

        return "${call.metaData.callerId} (${call.metaData.number})"
    }
}
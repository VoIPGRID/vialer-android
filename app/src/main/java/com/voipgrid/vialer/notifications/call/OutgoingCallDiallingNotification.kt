package com.voipgrid.vialer.notifications.call

import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R
import nl.voipgrid.vialer_voip.core.call.Call

class OutgoingCallDiallingNotification(private val call : Call) : AbstractCallNotification() {

    /**
     * Build an outgoing call notification for during dialling.
     *
     */
    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
                .setContentTitle(createNotificationTitle())
                .setContentText(context.getString(R.string.call_state_calling))
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(call.metaData.number))
    }

    private fun createNotificationTitle() : String {
        val dialing = context.getString(R.string.callnotification_dialing)

        if (call.metaData.callerId.isNotBlank()) {
            return "$dialing ${call.metaData.callerId} (${call.metaData.number})"
        }

        return "$dialing ${call.metaData.number}"
    }
}
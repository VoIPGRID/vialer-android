package com.voipgrid.vialer.notifications.call

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.CallActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.CallingConstants
import com.voipgrid.vialer.sip.SipCall


class ActiveCallNotification(private val call : SipCall) : AbstractCallNotification() {

    /**
     * Build the active call notification, this includes the call duration so
     * it is expected that this is called every second during an active call..
     *
     */
    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
                .setContentTitle(createNotificationTitle())
                .setContentText(context.getString(R.string.callnotification_active_call))
                .setContentIntent(createCallActivityPendingIntent())
                .setSubText(call.prettyCallDuration)
                .setShowWhen(false)
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(call.phoneNumber))
    }

    private fun createCallActivityPendingIntent(): PendingIntent? {
        return createPendingIntent(AbstractCallActivity.createIntentForCallActivity(context))
    }

    /**
     * Generate the notification title, this will change depending on whether
     * there is a call id.
     *
     */
    private fun createNotificationTitle() : String {
        if (call.callerId == null || call.callerId.isEmpty()) {
            return if (call.phoneNumber == null) " " else call.phoneNumber
        }

        return "${call.callerId} (${call.phoneNumber})"
    }
}
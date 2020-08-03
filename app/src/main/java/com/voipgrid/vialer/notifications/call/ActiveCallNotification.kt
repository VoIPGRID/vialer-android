package com.voipgrid.vialer.notifications.call

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.voip.Call


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
                .setContentIntent(createCallActivityPendingIntent())
//                .setSubText(call.duration)
                .setShowWhen(false)
//                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(call.phoneNumber))
    }

    private fun createCallActivityPendingIntent(): PendingIntent? {
        return createPendingIntent(Intent())
//        return createPendingIntent(AbstractCallActivity.createIntentForCallActivity(
//                context,
//                CallActivity::class.java,
//                call.phoneNumberUri,
//                CallingConstants.TYPE_OUTGOING_CALL,
//                call.callerId,
//                call.phoneNumber
//        ))
    }

    /**
     * Generate the notification title, this will change depending on whether
     * there is a call id.
     *
     */
    private fun createNotificationTitle() : String {
        return "CreatenOtifiactiontitle"
//        if (call.callerId == null || call.callerId.isEmpty()) {
//            return if (call.phoneNumber == null) " " else call.phoneNumber
//        }
//
//        return "${call.callerId} (${call.phoneNumber})"
    }
}
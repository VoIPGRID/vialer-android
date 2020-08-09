package com.voipgrid.vialer.notifications.call

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R
import com.voipgrid.voip.Call

class OutgoingCallDiallingNotification(private val call : com.voipgrid.voip.Call) : AbstractCallNotification() {

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
//                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(call.phoneNumber))
    }

    private fun createNotificationTitle() : String {
        val dialing = context.getString(R.string.callnotification_dialing)
return dialing
//        if (!call.callerId.isNullOrEmpty()) {
//            return "$dialing ${call.callerId} (${call.phoneNumber})"
//        }
//
//        return "$dialing ${call.phoneNumber}"
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
}
package com.voipgrid.vialer.notifications.call

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.CallActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.calling.AbstractCallActivity
import org.openvoipalliance.phonelib.model.Session

class OutgoingCallDiallingNotification(private val call : Session) : AbstractCallNotification() {

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
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(call.phoneNumber))
    }

    private fun createNotificationTitle() : String {
        val dialing = context.getString(R.string.callnotification_dialing)

        if (!call.displayName.isNullOrEmpty()) {
            return "$dialing ${call.displayName} (${call.phoneNumber})"
        }

        return "$dialing ${call.phoneNumber}"
    }

    private fun createCallActivityPendingIntent(): PendingIntent? {
        return createPendingIntent(AbstractCallActivity.createIntentForCallActivity(
                context,
                CallActivity::class.java
        ))
    }
}
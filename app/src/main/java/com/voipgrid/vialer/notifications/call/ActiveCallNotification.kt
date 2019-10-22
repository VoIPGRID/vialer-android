package com.voipgrid.vialer.notifications.call

import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.CallActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.CallingConstants
import com.voipgrid.vialer.sip.SipCall
import com.voipgrid.vialer.voip.core.call.Call


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
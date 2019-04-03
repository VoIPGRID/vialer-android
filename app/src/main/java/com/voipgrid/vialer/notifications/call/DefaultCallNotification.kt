package com.voipgrid.vialer.notifications.call

import androidx.core.app.NotificationCompat

class DefaultCallNotification : AbstractCallNotification() {

    /**
     * Build the default call notification, this is what will be shown temporarily
     * while the SipService has been activated with no call events having occurred.
     *
     */
    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
                .setContentTitle(" ")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSound(null)
    }
}
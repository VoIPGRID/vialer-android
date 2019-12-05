package com.voipgrid.vialer.sip.service

import android.app.Notification
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.notifications.call.AbstractCallNotification
import com.voipgrid.vialer.notifications.call.ActiveCallNotification
import com.voipgrid.vialer.notifications.call.DefaultCallNotification
import com.voipgrid.vialer.notifications.call.IncomingCallNotification
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.service.SipServiceNotificationManager.Type.*

class SipServiceNotificationManager(private val sipService: SipService) {

    private val logger = Logger(this)

    var notification: AbstractCallNotification = DefaultCallNotification()
        private set

    val active: AbstractCallNotification
        get() = notification

    /**
     * Updates the notification and sets the active notification appropriately. All notification changes should be published
     * via this method.
     *
     * @param notification
     */
    fun change(type: Type) {
        val notification = toNotification(type)
        logger.i("Received change notification request from: " + notification.javaClass.simpleName)
        if (shouldUpdateNotification(notification)) {
            logger.i("Performing notification change to" + notification.javaClass.simpleName)
            this.notification = notification
            sipService.startForeground(notification.notificationId, notification.build())
        }
    }

    fun toNotification(type: Type): AbstractCallNotification = when(type) {
        INCOMING -> notification.incoming(sipService.currentCall?.phoneNumber ?: "", sipService.currentCall?.callerId ?: "")
        ACTIVE -> notification.active(sipService.currentCall ?: throw Exception("No call for active call notification"))
        OUTGOING -> notification.outgoing(sipService.currentCall ?: throw Exception("No call for active call notification"))
    }

    /**
     * Check if the notification should be updated.
     *
     * @param notification
     * @return
     */
    private fun shouldUpdateNotification(notification: AbstractCallNotification): Boolean {
        if (this.notification.javaClass != notification.javaClass) return true

        if (notification.javaClass == ActiveCallNotification::class.java) {
            notification.display()
        }

        return false
    }

    enum class Type {
        INCOMING, ACTIVE, OUTGOING
    }
}
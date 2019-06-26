package com.voipgrid.vialer.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.voipgrid.vialer.VialerApplication

abstract class AbstractNotification {

    private val notificationManager : NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    protected val context : Context
        get() = VialerApplication.get()

    protected abstract val notificationId : Int

    /**
     * Build and display this notification.
     *
     */
    fun display() {
        notificationManager.notify(notificationId, build())
    }

    /**
     * Build the notification.
     *
     */
    fun build(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(buildChannel(context))
        }

        return buildNotification(context)
    }

    /**
     * Remove this notification.
     *
     */
    fun remove() {
        notificationManager.cancel(notificationId)
    }

    protected abstract fun buildNotification(context: Context): Notification

    @RequiresApi(value = 26)
    protected abstract fun buildChannel(context: Context): NotificationChannel
}
package com.voipgrid.vialer.notifications.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.contacts.PhoneNumberImageGenerator
import com.voipgrid.vialer.notifications.AbstractNotification
import nl.voipgrid.vialer_voip.core.call.Call
import javax.inject.Inject


/**
 * This is the SIP notification that will always be displayed while the sip service is running.
 *
 */
abstract class AbstractCallNotification : AbstractNotification() {

    /**
     * The id of all call notifications, this will mean they override each other which is the
     * intended behaviour.
     */
    public override val notificationId = 534

    /**
     * The small logo to display for all call notifications.
     *
     */
    private val logo = R.drawable.ic_phone_white

    @Inject protected lateinit var phoneNumberImageGenerator : PhoneNumberImageGenerator

    init {
        VialerApplication.get().component().inject(this)
    }

    /**
     * Build the ongoing calls channel, these notifications are not high priority
     * as they are really only for user information if the user leaves Vialer during
     * a call.
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_incoming_calls),
                NotificationManager.IMPORTANCE_HIGH
        )

        channel.enableVibration(false)
        channel.setSound(null, null)

        return channel
    }

    /**
     * Builds the notification using the defaults for call notifications.
     *
     */
    override fun buildNotification(context: Context): Notification {
        return applyUniqueNotificationProperties(applyCallNotificationDefaults(createNotificationBuilder())).build()
    }

    /**
     * Applies default properties that will affect all call notifications.
     *
     */
    private fun applyCallNotificationDefaults(builder : NotificationCompat.Builder) : NotificationCompat.Builder {
        return builder.setColor(context.resources.getColor(R.color.color_primary_dark))
                .setContentIntent(createCallActivityPendingIntent())
                .setColorized(true)
                .setSmallIcon(logo)
                .setOngoing(true)
    }

    /**
     * Should be overidden to provide the custom properties that will be unique to each type
     * of call notification.
     *
     */
    abstract fun applyUniqueNotificationProperties(builder : NotificationCompat.Builder) : NotificationCompat.Builder

    /**
     * Create a new instance of the notification builder, this can be overridden to provide
     * a custom channel id.
     *
     */
    open fun createNotificationBuilder() : NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
    }

    /**
     * Create a pending intent from an intent.
     *
     */
    private fun createPendingIntent(intent : Intent) : PendingIntent {
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    protected fun createCallActivityPendingIntent(): PendingIntent = createPendingIntent(Intent(context, NewCallActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })

    /**
     * Transform the call notification to an incoming call notification.
     *
     */
    fun incoming(number: String, callerId: String) {
        IncomingCallNotification(number, callerId).display()
    }

    /**
     * Transform the call notification to an ongoing call notification.
     *
     */
    fun outgoing(call : Call) {
        OutgoingCallDiallingNotification(call).display()
    }

    fun active(call : Call) {
        ActiveCallNotification(call).display()
    }

    companion object {
        const val CHANNEL_ID: String = "vialer_calling"
    }
}
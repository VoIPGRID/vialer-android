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
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallVibration
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.CallingConstants
import com.voipgrid.vialer.calling.IncomingCallActivity
import com.voipgrid.vialer.contacts.PhoneNumberImageGenerator
import com.voipgrid.vialer.notifications.AbstractNotification
import com.voipgrid.vialer.sip.SipCall
import com.voipgrid.vialer.sip.SipUri
import com.voipgrid.vialer.util.PhoneNumberUtils
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
    private val logo = R.drawable.ic_logo

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
        return NotificationChannel(
                CHANNEL_ID,
                context.getString(com.voipgrid.vialer.R.string.notification_channel_calls),
                NotificationManager.IMPORTANCE_MIN
        )
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
     * Create a pending intent to open the incoming call activity screen.
     *
     */
    protected fun createIncomingCallActivityPendingIntent(number: String, callerId: String): PendingIntent {
        return createPendingIntent(AbstractCallActivity.createIntentForCallActivity(
                context,
                IncomingCallActivity::class.java,
                SipUri.sipAddressUri(context, PhoneNumberUtils.format(number)),
                CallingConstants.TYPE_INCOMING_CALL,
                callerId,
                number
        ))
    }

    /**
     * Create a pending intent from an intent.
     *
     */
    protected fun createPendingIntent(intent : Intent) : PendingIntent {
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Transform the call notification to an incoming call notification.
     *
     */
    fun incoming(number: String, callerId: String) : AbstractCallNotification = IncomingCallNotification(number, callerId)

    /**
     * Transform the call notification to an ongoing call notification.
     *
     */
    fun outgoing(call : SipCall) : AbstractCallNotification = OutgoingCallDiallingNotification(call)

    /**
     * Transform the call notification to an active call notification.
     *
     */
    fun active(call : SipCall) : AbstractCallNotification = ActiveCallNotification(call)

    fun missed(number: String, contactName: String?) {
        MissedCallNotification(number, contactName).display()
    }

    fun answeredElsewhere(number: String) {
        AnsweredElsewhereNotification(number).display()
    }

    companion object {
        const val CHANNEL_ID: String = "vialer_calls"
    }
}
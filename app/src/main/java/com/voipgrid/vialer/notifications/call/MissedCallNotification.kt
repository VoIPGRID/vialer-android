package com.voipgrid.vialer.notifications.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.sip.SipConstants.EXTRA_CONTACT_NAME
import com.voipgrid.vialer.sip.SipConstants.EXTRA_PHONE_NUMBER
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.SipService.Actions.HANDLE_OUTGOING_CALL
import com.voipgrid.vialer.sip.SipUri
import com.voipgrid.vialer.util.PhoneNumberUtils
import java.util.*


class MissedCallNotification(private val number: String, private val contactName: String?) : AbstractCallNotification() {

    override val notificationId = (Date().time / 1000L % Integer.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID: String = "missed_notification"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        return NotificationChannel(
                CHANNEL_ID,
                "Call missed",
                NotificationManager.IMPORTANCE_HIGH
        )
    }

    override fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
    }

    override fun applyUniqueNotificationProperties(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        val intent = Intent(builder.mContext, MainActivity::class.java).apply {
            putExtra(MainActivity.Extra.NAVIGATE_TO.name, R.id.navigation_call_records)
        }
        val pendingIntent = PendingIntent.getActivity(builder.mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val bundle = Bundle()
        val uri = SipUri.sipAddressUri(
                context,
                PhoneNumberUtils.format(number)
        )
        bundle.putString(EXTRA_PHONE_NUMBER, number)
        bundle.putString(EXTRA_CONTACT_NAME, contactName ?: number)
        return builder
                .setOngoing(false)
                .setContentTitle(context.getString(R.string.missed_call, number))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .addAction(
                        R.drawable.ic_call_handle,
                        context.getString(R.string.call_back),
                        SipService.createSipServiceAction(HANDLE_OUTGOING_CALL, uri, bundle)
                )
    }
}
package com.voipgrid.voip.android

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.voipgrid.contacts.PhoneNumberImageGenerator
import com.voipgrid.voip.Call
import com.voipgrid.voip.R
import com.voipgrid.voip.SoftPhone
import nl.spindle.phonelib.PhoneLib
import nl.spindle.phonelib.model.Reason
import org.koin.core.KoinComponent
import org.koin.core.inject

class Connection: android.telecom.Connection(), KoinComponent {

    private val softPhone: SoftPhone by inject()
    private val context: Context by inject()
    private val notificationManager: NotificationManager by inject()
    private val phoneNumberImageGenerator: PhoneNumberImageGenerator by inject()

    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()

        val number = "1800112113"
        val id = "INCOMING_CALL_NOTIFICATION_CHANNEL"

        val channel = NotificationChannel(id, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH)
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        channel.setSound(ringtoneUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.flags = Intent.FLAG_ACTIVITY_NO_USER_ACTION and FLAG_ACTIVITY_NEW_TASK
        intent.setClass(context, Class.forName("com.voipgrid.vialer.call.ui.IncomingCallActivity"))
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, 0)

        val notification: Notification = Notification.Builder(context)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setChannelId(id)
                .setContentTitle("Incoming call from ${softPhone.call?.display?.heading}")
                .setContentText(softPhone.call?.display?.subheading)
                .setSmallIcon(R.drawable.ic_logo)
                .setLargeIcon(phoneNumberImageGenerator.findWithRoundedCorners(number))
                .addAction(
                        Notification.Action.Builder(
                                R.drawable.ic_call_decline_normal,
                                "Decline",
                                PendingIntent.getBroadcast(context, 1, Intent(context, NotificationActionReceiver::class.java).apply {
                                    action = NotificationActionReceiver.Action.DECLINE.toString()
                                }, 0)
                        ).build()
                )
                .addAction(
                        Notification.Action.Builder(
                                R.drawable.ic_call_answer_normal,
                                "Accept",
                                PendingIntent.getBroadcast(context, 1, Intent(context, NotificationActionReceiver::class.java).apply {
                                    action = NotificationActionReceiver.Action.ACCEPT.toString()
                                }, 0)
                        ).build()
                )
                .build()

        notification.flags = notification.flags and Notification.FLAG_INSISTENT
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    override fun onAnswer() {
        super.onAnswer()

        softPhone.call?.let {
            softPhone.actions.acceptIncoming(it.session)
        }

        notificationManager.cancel(NOTIFICATION_ID)
        setActive()
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)

        if (state == STATE_ACTIVE) {
            context.startActivity(Intent(context, Class.forName("com.voipgrid.vialer.call.ui.CallActivity")).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
        Log.e("TEST123", "Audio state change... $state")
    }

    override fun onHold() {
        super.onHold()
        softPhone.call?.let {
            softPhone.actions.setHold(it.session, true)
        }
        setOnHold()
    }

    override fun onUnhold() {
        super.onUnhold()
        softPhone.call?.let {
            softPhone.actions.setHold(it.session, false)
        }
        setActive()
    }

    @SuppressLint("MissingPermission")
    override fun onReject() {
        super.onReject()
        softPhone.call?.let {
            softPhone.actions.declineIncoming(it.session, Reason.DECLINED)
        }
        notificationManager.cancel(NOTIFICATION_ID)
        destroy()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        softPhone.call?.let {
            if (it.state != Call.State.ENDED) {
                softPhone.actions.end(it.session)
            }
        }
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        notificationManager.cancel(NOTIFICATION_ID)
        destroy()
    }

    companion object {
        const val NOTIFICATION_ID = 5373
    }
}
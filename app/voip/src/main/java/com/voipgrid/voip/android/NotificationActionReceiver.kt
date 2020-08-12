package com.voipgrid.voip.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.voipgrid.voip.SoftPhone
import com.voipgrid.voip.android.NotificationActionReceiver.Action.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class NotificationActionReceiver: BroadcastReceiver(), KoinComponent {

    private val softPhone: SoftPhone by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = valueOf(intent.action ?: return)

        when (action) {
            ACCEPT -> softPhone.call?.connection?.onAnswer()
            DECLINE -> softPhone.call?.connection?.onReject()
        }
    }

    enum class Action {
        ACCEPT, DECLINE
    }
}
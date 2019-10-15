package com.voipgrid.vialer.voip.middleware

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.voip.VoipService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CallPushHandler(private val context: Context, private val middleware: Middleware) : PushMessageHandler {

    private lateinit var voip: VoipService

    override fun handle(pushMessage: RemoteMessage, voip: VoipService?) {
        this.voip = voip ?: return

        if (!voip.canHandleIncomingCall()) {
            middleware.replyToIncomingCall(pushMessage, false)
            return
        }

        context.startService(Intent(context, VoipService::class.java))
        handleCall(pushMessage)
    }

    private fun handleCall(pushMessage: RemoteMessage) = GlobalScope.launch(Dispatchers.Main) {
        voip.prepareForIncomingCall {
            middleware.replyToIncomingCall(pushMessage, true)
        }
    }
}
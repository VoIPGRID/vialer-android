package com.voipgrid.vialer.voip.middleware

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.User.middleware
import com.voipgrid.vialer.api.Middleware
import com.voipgrid.vialer.voip.VoipService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CallPushHandler(private val context: Context, private val middleware: Middleware) : PushMessageHandler {

    private lateinit var voip: VoipService

    override fun handle(pushMessage: RemoteMessage, voip: VoipService?) {
        this.voip = voip ?: return
Log.e("TEST123", "Call handler!")
        if (!voip.canHandleIncomingCall()) {
            replyToMiddleware(pushMessage, false)   // TODO: Refactor so we have a middlware class that can handle replying, and registering and everything else
            return
        }

        context.startService(Intent(context, VoipService::class.java))
        handleCall(pushMessage)
    }

    private fun handleCall(pushMessage: RemoteMessage) = GlobalScope.launch(Dispatchers.Main) {
        voip.prepareForIncomingCall {
            replyToMiddleware(pushMessage, true)
        }
    }

    private fun replyToMiddleware(pushMessage: RemoteMessage, isAvailable: Boolean) = GlobalScope.launch {
        middleware.reply(pushMessage.data["unique_key"], isAvailable, pushMessage.data["message_start_time"]).execute()
    }
}
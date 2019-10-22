package com.voipgrid.vialer.voip.middleware

import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.voip.middleware.MiddlewareMessageReceiver.Type.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.util.*

class MiddlewareMessageReceiver : VoipBoundFcm() {

    private val middleware: Middleware by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        Log.e("TEST123", "Middleware has received push notification")
        val messageType = valueOf(message.data["type"]?.toUpperCase(Locale.ENGLISH) ?: return)

        when(messageType) {
            MESSAGE -> MessagePushHandler()
            CALL -> CallPushHandler(get(), get())
        }.handle(message, voip)
    }

    override fun onNewToken(token: String)  {
        middleware.register(token)
    }

    enum class Type {
        MESSAGE, CALL
    }
}
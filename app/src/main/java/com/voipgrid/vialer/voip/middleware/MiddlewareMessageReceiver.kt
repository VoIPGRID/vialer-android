package com.voipgrid.vialer.voip.middleware

import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.voip.middleware.MiddlewareMessageReceiver.Type.*
import org.koin.android.ext.android.get
import java.util.*

class MiddlewareMessageReceiver : VoipBoundFcm() {

    override fun onMessageReceived(message: RemoteMessage) {
        val messageType = valueOf(message.data["type"]?.toUpperCase(Locale.ENGLISH) ?: return)

        when(messageType) {
            MESSAGE -> MessagePushHandler()
            CALL -> CallPushHandler(get(), get())
        }.handle(message, voip)
    }

    override fun onNewToken(token: String) {
    }

    enum class Type {
        MESSAGE, CALL
    }
}
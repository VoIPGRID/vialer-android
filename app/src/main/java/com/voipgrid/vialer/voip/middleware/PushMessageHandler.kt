package com.voipgrid.vialer.voip.middleware

import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.voip.VoipService

interface PushMessageHandler {

    fun handle(pushMessage: RemoteMessage, voip: VoipService?)
}
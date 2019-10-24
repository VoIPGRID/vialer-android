package nl.voipgrid.vialer_voip.middleware

import com.google.firebase.messaging.RemoteMessage
import nl.voipgrid.vialer_voip.VoipService

interface PushMessageHandler {

    fun handle(pushMessage: RemoteMessage, voip: VoipService?)
}
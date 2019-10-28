package nl.voipgrid.vialer_voip.android

import android.content.Intent
import android.os.Handler
import android.util.Log
import nl.voipgrid.vialer_voip.Voip
import nl.voipgrid.vialer_voip.core.audio.AudioRouter
import nl.voipgrid.vialer_voip.core.call.Call
import nl.voipgrid.vialer_voip.core.call.State
import nl.voipgrid.vialer_voip.core.eventing.Event
import org.koin.android.ext.android.inject

class VoipService : BindableService<VoipService>()  {

    private val voip: Voip by inject()

    override fun onCreate() {
        super.onCreate()
//        voip.init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startForeground(notification.notificationId, notification.build())
        return START_NOT_STICKY
    }







    override fun onDestroy() {
        voip.destroy()
    }

    override fun self(): VoipService = this
}
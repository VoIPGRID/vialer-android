package com.voipgrid.vialer.voip.middleware

import android.content.*
import android.os.IBinder
import com.google.firebase.messaging.FirebaseMessagingService
import com.voipgrid.vialer.voip.VoipService
import com.voipgrid.vialer.voip.android.BindableService

abstract class VoipBoundFcm : FirebaseMessagingService() {

    protected var voip: VoipService? = null
    private var bound = false

    private val connection = object : ServiceConnection {

        @Suppress("UNCHECKED_CAST")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as? BindableService<VoipService>.LocalBinder ?: return
            voip = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        Intent(this, VoipService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
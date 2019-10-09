package com.voipgrid.vialer.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.voip.VoipService
import kotlinx.android.synthetic.main.activity_incoming_call.*

class NewIncomingCallActivity : LoginRequiredActivity() {

    private var voip: VoipService? = null

    private val connection = object : ServiceConnection {
        private var bound = false

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as VoipService.LocalBinder
            voip = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Intent(this, VoipService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        setContentView(R.layout.activity_incoming_call)
        incoming_caller_title.text = voip?.getCurrentCall()?.metaData?.number
        incoming_caller_title.text = voip?.getCurrentCall()?.metaData?.callerId
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
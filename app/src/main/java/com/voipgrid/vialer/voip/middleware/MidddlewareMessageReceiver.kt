package com.voipgrid.vialer.voip.middleware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.User.voip
import com.voipgrid.vialer.api.Middleware
import com.voipgrid.vialer.voip.VoipService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class MidddlewareMessageReceiver : FirebaseMessagingService() {

    private var voip: VoipService? = null

    private val middleware: Middleware by inject()

    private lateinit var message: RemoteMessage


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

    override fun onMessageReceived(message: RemoteMessage) {
        val token = message.data["unique_key"] ?: return

        if (handling.contains(token)) return
        Log.e("TEST123", "middleware on ${message.data["phonenumber"]} and ${message.data["caller_id"]}")
        handling.add(token)
        val voip = this.voip ?: return
        this.message = message

        if (!voip.canHandleIncomingCall()) {
            replyToMiddleware(false)
            return
        }
        startService(Intent(this, VoipService::class.java))

        handleCall()
    }

    private fun handleCall() = GlobalScope.launch {
        voip?.let {
            it.prepareForIncomingCall()
            replyToMiddleware(true)
        }
    }

    private fun replyToMiddleware(isAvailable: Boolean) = GlobalScope.launch(Dispatchers.IO) {
        middleware.reply(message.data["unique_key"], isAvailable, message.data["message_start_time"]).execute()
    }

    override fun onNewToken(token: String) {
    }

    companion object {

        private val handling = mutableListOf<String>()
    }
}
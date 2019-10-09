package com.voipgrid.vialer.voip

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.notifications.call.DefaultCallNotification
import com.voipgrid.vialer.voip.core.*
import com.voipgrid.vialer.voip.core.call.Call
import com.voipgrid.vialer.voip.providers.pjsip.core.PjsipCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class VoipService : Service(), IncomingCallListener {

    private val voipProvider: VoipProvider by inject()
    private val incomingCallHandler: IncomingCallHandler by inject()

    private val notification = DefaultCallNotification()

    private val callStack = CallStack()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("TEST123", "onStartCommand")
        voipProvider.initialize(generateConfiguration(), this)
        return START_STICKY
    }

    suspend fun call(number: String) = withContext(Dispatchers.IO) {
        val call = voipProvider.call(number)

        callStack.add(call)
    }

    fun getCurrentCall(): Call = callStack.last()

    fun canHandleIncomingCall(): Boolean {
        return true
    }

    suspend fun prepareForIncomingCall() {
        voipProvider.initialize(generateConfiguration(), this)
        voipProvider.register()
    }

    /**
     * This is called when there is an actual call coming through via sip. TODO: make this obvious with method name
     *
     */
    override fun onIncomingCall(call: Call) {
        callStack.add(call)

        this.incomingCallHandler.handle(call, notification)
    }

    private fun generateConfiguration(): Configuration = Configuration(
            host = SipHost(getString(if (User.voip.hasTlsEnabled) R.string.sip_host_secure else R.string.sip_host)),
            accountId = User.voipAccount?.accountId ?: "",
            password = User.voipAccount?.password ?: "",
            scheme = getString(R.string.sip_auth_scheme),
            realm = getString(R.string.sip_auth_realm),
            transport = if (User.voip.hasTlsEnabled) Configuration.Transport.TLS else Configuration.Transport.TCP,
            stun = User.voip.hasStunEnabled,
            userAgent = "vialer-test-ua",
            echoCancellation = 3,
            echoCancellationTailLength = 75,
            codec = Configuration.Codec.OPUS
    )


    override fun onDestroy() {
        this.voipProvider.destroy()
    }


    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? = binder

    inner class LocalBinder : Binder() {
        fun getService(): VoipService = this@VoipService
    }
}
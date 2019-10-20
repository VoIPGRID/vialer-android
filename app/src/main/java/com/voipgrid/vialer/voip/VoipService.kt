package com.voipgrid.vialer.voip

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.notifications.call.DefaultCallNotification
import com.voipgrid.vialer.voip.core.*
import com.voipgrid.vialer.voip.core.call.Call
import com.voipgrid.vialer.voip.core.call.State
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class VoipService : Service(), VoipListener {

    private val voipProvider: VoipProvider by inject()
    private val incomingCallHandler: IncomingCallHandler by inject()
    val audio: AudioRouter by inject()
    private val broadcastManager: LocalBroadcastManager by inject()

    private val notification = DefaultCallNotification()

    override fun onCreate() {
        super.onCreate()
        callStack = CallStack()
        onTic()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    fun call(number: String) {
        val call = voipProvider.call(number)

        callStack.add(call)
    }

    fun getCurrentCall(): Call?  = try { callStack.last() } catch (_: Exception) { null }

    fun canHandleIncomingCall() = true

    private var onPrepared: (() -> Unit)? = null

    fun prepareForIncomingCall(onPrepared: () -> Unit) {
        this.onPrepared = onPrepared

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

        if (audio.isBluetoothRouteAvailable && !audio.isCurrentlyRoutingAudioViaBluetooth) {
            audio.routeAudioViaBluetooth()
        }
    }

    override fun onCallStateUpdate(call: Call, state: State) {
        when(state.telephonyState) {
            State.TelephonyState.INITIALIZING -> {}
            State.TelephonyState.CALLING -> {}
            State.TelephonyState.RINGING -> {}
            State.TelephonyState.CONNECTED -> { }
            State.TelephonyState.DISCONNECTED -> removeCallFromStack(call)
        }

        broadcastManager.sendBroadcast(Intent(ACTION_CALL_STATE_WAS_UPDATED).apply {
            putExtra(CALL_STATE_EXTRA, state.telephonyState)
        })
    }

    private fun removeCallFromStack(call: Call) {
        callStack.remove(call)

        if (callStack.isEmpty()) {

            Log.e("TEST123", " Call stack is empty stopSelf")
            stopForeground(true)
            stopSelf()
        }
    }

    private fun onTic() {
        broadcastManager.sendBroadcast(Intent(ACTION_VOIP_UPDATE))
        Handler().postDelayed({
            onTic()
        }, 500)
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
        Log.e("TEST123", " onDestroy")
        this.voipProvider.destroy()
        this.audio.destroy()
    }

    override fun onRegister() {
        onPrepared?.invoke()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? = binder

    inner class LocalBinder : Binder() {
        fun getService(): VoipService = this@VoipService
    }

    companion object {
        private var callStack = CallStack()

        const val ACTION_CALL_STATE_WAS_UPDATED = "CALL_STATE_WAS_UPDATED"
        const val CALL_STATE_EXTRA = "CALL_STATE_EXTRA"
        const val ACTION_VOIP_UPDATE = "ACTION_VOIP_UPDATE"
    }
}
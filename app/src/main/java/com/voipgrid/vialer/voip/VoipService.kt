package com.voipgrid.vialer.voip

import android.content.Intent
import android.os.Handler
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.notifications.call.DefaultCallNotification
import com.voipgrid.vialer.voip.android.BindableService
import com.voipgrid.vialer.voip.core.*
import com.voipgrid.vialer.voip.core.call.Call
import com.voipgrid.vialer.voip.core.call.State
import org.koin.android.ext.android.inject

class VoipService : BindableService<VoipService>(), VoipListener {

    private val voipProvider: VoipProvider by inject()
    val audio: AudioRouter by inject()
    private val broadcastManager: LocalBroadcastManager by inject()
    private val notification = DefaultCallNotification()

    private var onPrepared: (() -> Unit)? = null

    val calls = CallStack()

    override fun onCreate() {
        super.onCreate()
        onTic()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notification.notificationId, notification.build())
        return START_NOT_STICKY
    }

    /**
     * Place a call to the given number, an event will be emitted upon setup
     * success.
     *
     */
    fun call(number: String) = call(number, false)

    /**
     * Place a call to the given number.
     *
     */
    private fun call(number:String, isTransferTarget: Boolean)= prepare {
        val call = voipProvider.call(number)

        call.state.isTransferTarget = isTransferTarget

        calls.add(call)

        this.broadcastManager.sendBroadcast(Intent(Events.OUTGOING_CALL_HAS_BEEN_SETUP.name))
    }


    /**
     * Determine if the service is able to handle an incoming call.
     *
     */
    fun isAvailableToHandleIncomingCall() = calls.isEmpty()

    /**
     * Prepare the library for a call to be made. The callback is invoked when the library has been prepared.
     *
     */
    fun prepare(onPrepared: () -> Unit) {
        this.onPrepared = onPrepared

        voipProvider.apply {
            initialize(configuration.invoke(), this@VoipService)
            register(credentials.invoke())
        }
    }

    /**
     * Initiate a transfer to the given number.
     *
     */
    fun initiateTransfer(number: String) {
        if (calls.isEmpty()) {
            throw Exception("Cannot begin initiateTransfer without an active call")
        }

        call(number, true)
    }

    /**
     * Merge the two calls that are currently queued for transfer.
     *
     */
    fun mergeTransfer() {
        if (!isTransferring()) {
            throw Exception("Unable to merge call transfer as not transferring calls")
        }

        voipProvider.mergeTransfer(calls.original ?: return, calls.original ?: return)
    }

    /**
     * Determine if there is a transfer in progress currently.
     *
     */
    fun isTransferring() = calls.size > 1

    /**
     * This is called when there is an actual call coming through via sip. TODO: make this obvious with method name
     *
     */
    override fun onIncomingCallFromVoipProvider(call: Call) {
        calls.add(call)

        if (audio.isBluetoothRouteAvailable && !audio.isCurrentlyRoutingAudioViaBluetooth) {
            audio.routeAudioViaBluetooth()
        }

        this.broadcastManager.sendBroadcast(Intent(Events.INCOMING_CALL_IS_RINGING.name))
    }

    override fun onCallStateUpdate(call: Call, state: State) {
        when(state.telephonyState) {
            State.TelephonyState.INITIALIZING -> {}
            State.TelephonyState.OUTGOING_CALLING -> {}
            State.TelephonyState.INCOMING_RINGING -> {}
            State.TelephonyState.CONNECTED -> {
                this.audio.focus()
            }
            State.TelephonyState.DISCONNECTED -> removeCallFromStack(call)
        }

        broadcastManager.sendBroadcast(Intent(Events.CALL_STATE_HAS_CHANGED.name).apply {
            putExtra(Extras.CALL_STATE.name, state.telephonyState)
        })
    }

    /**
     * Remove a call from the call stack, and stop the service if we have no
     * more calls left.
     *
     */
    private fun removeCallFromStack(call: Call) {
        calls.remove(call)

        if (calls.isEmpty()) {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun onTic() {
        broadcastManager.sendBroadcast(Intent(Events.VOIP_TIC.name))
        Handler().postDelayed({
            onTic()
        }, 500)

        val call = calls.original ?: return

        when (call.state.telephonyState) {
            State.TelephonyState.INCOMING_RINGING -> notification.incoming(call.metaData.number, call.metaData.callerId)
            State.TelephonyState.OUTGOING_CALLING -> notification.outgoing(call)
            State.TelephonyState.CONNECTED -> notification.active(call)
            else -> {}
        }
    }

    override fun onDestroy() {
        Log.e("TEST123", " onDestroy")
        this.voipProvider.destroy()
        this.audio.destroy()
    }

    override fun onRegister() {
        onPrepared?.invoke()
        onPrepared = null
    }

    override fun self(): VoipService = this

    companion object {
        lateinit var configuration: () -> Configuration
        lateinit var credentials: () -> Credentials
    }

    enum class Events {
        OUTGOING_CALL_HAS_BEEN_SETUP, INCOMING_CALL_IS_RINGING, CALL_STATE_HAS_CHANGED, VOIP_TIC
    }

    enum class Extras {
        CALL_STATE
    }
}
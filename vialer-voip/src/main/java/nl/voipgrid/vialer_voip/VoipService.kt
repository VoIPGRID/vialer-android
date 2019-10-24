package nl.voipgrid.vialer_voip

import android.content.Intent
import android.os.Handler
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.voipgrid.vialer_voip.android.BindableService
import nl.voipgrid.vialer_voip.android.Events
import nl.voipgrid.vialer_voip.android.Extras
import nl.voipgrid.vialer_voip.android.audio.AudioRouter
import nl.voipgrid.vialer_voip.core.*
import nl.voipgrid.vialer_voip.core.call.Call
import nl.voipgrid.vialer_voip.core.call.State
import org.koin.android.ext.android.inject

class VoipService : BindableService<VoipService>(), VoipListener {

    private val voipProvider: VoipProvider by inject()
    val audio: AudioRouter by inject()
    private val broadcastManager: LocalBroadcastManager by inject()

    private var onPrepared: (() -> Unit)? = null

    val calls = CallStack()

    override fun onCreate() {
        super.onCreate()
        onTic()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startForeground(notification.notificationId, notification.build())
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

        broadcast(Events.OUTGOING_CALL_HAS_BEEN_SETUP)
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
            initialize(Voip.configuration.invoke(), this@VoipService)
            register(Voip.credentials.invoke())
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

        voipProvider.connectCalls(calls.original ?: return, calls.original ?: return)
    }

    /**
     * Determine if there is a transfer in progress currently.
     *
     */
    fun isTransferring() = calls.size > 1

    /**
     * This is called when there is an actual call coming through via sip.
     *
     */
    override fun onIncomingCallFromVoipProvider(call: Call) {
        calls.add(call)

        if (audio.isRouteAvailable(AudioRouter.Route.BLUETOOTH) && !audio.isCurrentlyRoutingVia(AudioRouter.Route.BLUETOOTH)) {
            audio.routeVia(AudioRouter.Route.BLUETOOTH)
        }

        broadcast(Events.INCOMING_CALL_IS_RINGING)
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

        broadcast(Events.CALL_STATE_HAS_CHANGED, mapOf(
            Extras.CALL_STATE to state.telephonyState.name,
            Extras.PREVIOUS_STATE to state.previousTelephonyState.name
        ))
    }

    /**
     * Broadcast an event and properties.
     *
     */
    private fun broadcast(event: Events, extras: Map<Extras, String> = mapOf()) {
        broadcastManager.sendBroadcast(Intent(event.name).apply {
            extras.forEach { extra ->
                putExtra(extra.key.name, extra.value)
            }
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
    }

    override fun onDestroy() {
        Log.e("TEST123", " onDestroy")
        this.voipProvider.destroy()
        this.audio.destroy()
    }

    override fun onVoipProviderRegistered() {
        onPrepared?.invoke()
        onPrepared = null
    }

    override fun self(): VoipService = this
}
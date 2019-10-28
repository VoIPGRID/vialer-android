package nl.voipgrid.vialer_voip

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.voipgrid.vialer_voip.core.audio.AudioRouter
import nl.voipgrid.vialer_voip.core.*
import nl.voipgrid.vialer_voip.core.call.Call
import nl.voipgrid.vialer_voip.core.call.State
import nl.voipgrid.vialer_voip.core.eventing.*
import org.koin.core.context.loadKoinModules

class Voip(private val driver: VoipDriver, private val eventing: EventBroadcaster, private val audio: AudioRouter) : VoipListener {

    val calls = CallStack()

    private var onPrepared: (() -> Unit)? = null

    fun destroy() {
        this.driver.destroy()
        this.audio.destroy()
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
        val call = driver.call(number)

        call.state.isTransferTarget = isTransferTarget

        calls.add(call)

        eventing.broadcast(OutgoingCallWasSetup)
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

        driver.apply {
            initialize(Voip.configuration.invoke(), this@Voip)
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

        driver.connectCalls(calls.original ?: return, calls.original ?: return)
    }


    /**
     * Determine if there is a transfer in progress currently.
     *
     */
    fun isTransferring() = calls.size > 1


    /**
     * Remove a call from the call stack, and stop the service if we have no
     * more calls left.
     *
     */
    private fun removeCallFromStack(call: Call) {
        calls.remove(call)

        if (calls.isEmpty()) {
//            stopForeground(true) - propogate this back
//            stopSelf()
        }
    }

    /**
     * This is called when there is an actual call coming through via sip.
     *
     */
    override fun onIncomingCallFromVoipDriver(call: Call) {
        calls.add(call)

        if (audio.isRouteAvailable(AudioRouter.Route.BLUETOOTH) && !audio.isCurrentlyRoutingVia(AudioRouter.Route.BLUETOOTH)) {
            audio.routeVia(AudioRouter.Route.BLUETOOTH)
        }

        eventing.broadcast(IncomingCallStartedRinging)
    }

    override fun onCallStateUpdate(call: Call, state: State) {
        when(state.telephonyState) {
            State.TelephonyState.INITIALIZING -> {}
            State.TelephonyState.OUTGOING_CALLING -> {}
            State.TelephonyState.INCOMING_RINGING -> {}
            State.TelephonyState.CONNECTED -> {
//                this.audio.focus()
            }
            State.TelephonyState.DISCONNECTED -> removeCallFromStack(call)
        }

        eventing.broadcast(CallStateDidChange(state.telephonyState, state.previousTelephonyState))
    }



    override fun onVoipProviderRegistered() {
        onPrepared?.invoke()
        onPrepared = null
    }

    companion object {
        lateinit var configuration: () -> Configuration
        lateinit var credentials: () -> Credentials
    }
}


fun voip(config: () -> Configuration, credentials: () -> Credentials) {
    loadKoinModules(voipModule)
}
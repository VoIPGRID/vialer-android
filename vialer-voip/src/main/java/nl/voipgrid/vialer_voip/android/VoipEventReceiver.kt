package nl.voipgrid.vialer_voip.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.voipgrid.vialer_voip.android.Events.*
import nl.voipgrid.vialer_voip.core.call.State

abstract class VoipEventReceiver : BroadcastReceiver() {

    /**
     * Listen for an event from voip, if received then call the relevant abstract method.
     *
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = valueOf(intent?.action ?: return)

        when (action) {
            OUTGOING_CALL_HAS_BEEN_SETUP -> outgoingCallHasBeenSetup()
            INCOMING_CALL_IS_RINGING -> incomingCallHasStartedRinging()
            CALL_STATE_HAS_CHANGED -> performActionBasedOnCallState(intent)
            else -> {}
        }
    }

    /**
     * Determine the new call state, call a relevant method based on the call state.
     *
     *
     */
    private fun performActionBasedOnCallState(intent: Intent) {
        val state = (intent.extras?.get(Extras.CALL_STATE.name) as? State.TelephonyState) ?: return

        if (state != State.TelephonyState.INCOMING_RINGING) {
            incomingCallHasStoppedRinging()
        }
    }

    /**
     * An incoming call has started ringing, you will likely want to trigger a notification.
     *
     */
    abstract fun incomingCallHasStartedRinging()

    /**
     * To handle multiple outcomes this will be triggered even when a call was not ringing,
     * this should only perform actions to stop whatever was triggered in the [incomingCallHasStartedRinging].
     *
     */
    abstract fun incomingCallHasStoppedRinging()

    /**
     * An outgoing call has been setup and the call is ready to be displayed to the user.
     *
     */
    abstract fun outgoingCallHasBeenSetup()
}
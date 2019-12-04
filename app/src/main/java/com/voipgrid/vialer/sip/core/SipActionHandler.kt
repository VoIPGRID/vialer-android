package com.voipgrid.vialer.sip.core

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.permissions.MicrophonePermission
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.core.Action.*
import com.voipgrid.vialer.sip.incoming.MiddlewareResponse
import java.lang.Exception

class SipActionHandler(private val sipService: SipService) {

    private val logger = Logger(this)

    /**
     * Handle a giving sip action.
     *
     */
    fun handle(action: Action, intent: Intent): Unit = when(action) {
        HANDLE_INCOMING_CALL -> {
            sipService.pendingMiddlewareResponses.add(MiddlewareResponse(
                    intent.getStringExtra(SipService.Extra.INCOMING_TOKEN.name) ?: throw Exception("Unable to start an incoming call without a token"),
                    intent.getStringExtra(SipService.Extra.INCOMING_CALL_START_TIME.name) ?: throw Exception("Unable to start an incoming call without a call start time")
            ))
            Unit
        }
        HANDLE_OUTGOING_CALL -> sipService.placeOutgoingCall(intent.getStringExtra(SipService.Extra.OUTGOING_PHONE_NUMBER.toString()) ?: throw IllegalArgumentException("Unable to start a call without an outgoing number"))
        DECLINE_INCOMING_CALL -> SipService.connection.onReject()
        ANSWER_INCOMING_CALL -> askForPermissionsThenAnswer()
        END_CALL -> SipService.connection.onDisconnect()
        DISPLAY_CALL_IF_AVAILABLE -> showCallIfAvailable()
        SILENCE -> sipService.silence()
    }.also {
        logger.i("Executing Sip Action: ${action.name}")
    }

    /**
     * Returns TRUE if the action given should prompt a foreground start.
     *
     */
    fun isForegroundAction(action: Action): Boolean = listOf(HANDLE_INCOMING_CALL, HANDLE_OUTGOING_CALL).contains(action)

    /**
     * Start the call activity if there is a call in progress.
     *
     */
    private fun showCallIfAvailable() {
        if (sipService.currentCall != null && sipService.currentCall?.isConnected == true) {
            sipService.startCallActivityForCurrentCall()
        } else {
            Log.e("TEST123", "showClalif")
            sipService.stopSelf()
        }
    }

    /**
     * Prompt the user to accept the microphone permission if they do not have it, then answer the call.
     *
     */
    private fun askForPermissionsThenAnswer() {
        if (!MicrophonePermission.hasPermission(sipService))
        {
            Toast.makeText(sipService, sipService.getString(R.string.permission_microphone_missing_message), Toast.LENGTH_LONG).show()
            logger.e("Unable to answer incoming call as we do not have microphone permission")
            return
        }

        SipService.connection.onAnswer()
    }

    companion object {
        /**
         * Create an action for the SipService, specifying a valid action.
         *
         * @param action The action the SipService should perform when resolved
         * @return The complete pending intent
         */
        fun createSipServiceAction(action: Action): PendingIntent {
            val intent = Intent(VialerApplication.get(), SipService::class.java)
            intent.action = action.toString()
            return PendingIntent.getService(VialerApplication.get(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        /**
         * Given a context, this will directly perform an action on the SipService.
         *
         * @param context
         * @param action
         */
        @JvmStatic
        fun performActionOnSipService(context: Context, action: Action) {
            if (Action.DISPLAY_CALL_IF_AVAILABLE == action && !SipService.sipServiceActive) {
                return
            }
            val intent = Intent(context, SipService::class.java)
            intent.action = action.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
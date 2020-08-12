package com.voipgrid.vialer.call.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.CallAudioState
import android.telecom.Connection
import android.util.Log
import android.widget.Toast
import com.voipgrid.vialer.R
import com.voipgrid.vialer.call.ui.buttons.AudioRouteButton
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.voip.Call.State.*
import com.voipgrid.voip.SoftPhone
import kotlinx.android.synthetic.main.activity_call.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class CallActivity : LoginRequiredActivity(), KoinComponent {

    private val softPhone: SoftPhone by inject()

    val mainHandler = Handler(Looper.getMainLooper())
    val render = object : Runnable {
        override fun run() {
            render()
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        initialiseButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(render)
        softPhone.sessionUpdate = {
            render()
        }
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(render)
        softPhone.sessionUpdate = null
    }

    private fun initialiseButtonListeners() {
        val connection = softPhone.call?.connection ?: return
        val call = softPhone.call ?: return

        button_mute.setOnClickListener {
            softPhone.setMuteMicrophone(!softPhone.isMicrophoneMuted())
            render()
        }

        button_hangup.setOnClickListener {
            connection.onDisconnect()
        }

        button_onhold.setOnClickListener {
            if (call.isOnHold) {
                connection.onUnhold()
            } else {
                connection.onHold()
            }
        }

        button_transfer.setOnClickListener {
            Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show()
        }

        button_speaker.setOnClickListener {
            (it as AudioRouteButton).handleClick(connection, this)
            render()
        }
    }

    private fun render() {
        if (shouldCloseUi()) {
            finish()
            return
        }

        initialiseButtonListeners()
        val connection = softPhone.call?.connection ?: return
        val call = softPhone.call ?: return

        third_party_title.text = call.display.heading
        third_party_subtitle.text = call.display.subheading
        duration_text_view.text = when (call.state) {
            INITIALIZING -> "Initializing"
            RINGING -> "Ringing"
            CONNECTED -> softPhone.call?.display?.prettyDuration
            HELD_BY_LOCAL -> "On Hold"
            HELD_BY_REMOTE -> "Held by Remote"
            ENDED -> "Call Ended"
            ERROR -> "Call Error"
        }


        button_mute.activate(softPhone.isMicrophoneMuted())
        speaker_label.text = button_speaker.update(connection.callAudioState.route, connection.callAudioState.supportedRouteMask, connection.callAudioState.activeBluetoothDevice).toLowerCase()

        button_onhold.activate(connection.state == Connection.STATE_HOLDING)
        button_dialpad.activate(false)
        button_transfer.activate(false)
    }

    private fun shouldCloseUi(): Boolean {
        if (softPhone.call == null) return true

        if (softPhone.call?.connection == null) return true

        val endStates = listOf(ENDED, ERROR)

        if (endStates.contains(softPhone.call?.state)) return true

        return false
    }
}
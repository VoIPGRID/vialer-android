package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R
import com.voipgrid.vialer.audio.Routes
import com.voipgrid.vialer.call.CallActionButton
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.voip.core.call.State
import kotlinx.android.synthetic.main.fragment_call_active_buttons.*

class ActiveCallButtons : VoipAwareFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_active_buttons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arrayOf<CallActionButton>(button_dialpad, button_mute, button_hold, button_transfer).forEach { button ->
            button.setOnClickListener { handleCallAction(it) }
        }

        button_hangup.setOnClickListener {
            voip?.getCurrentCall()?.hangup()
        }

        voip?.audio?.let {
            button_audio_source.audio = it
        }

        button_audio_source.setOnHandleAudioSourceSelectionListener { handleAudioSourceSelected(it) }
    }

    private fun handleCallAction(button: View) {
        val voip = voip ?: return
        val call = voip.getCurrentCall() ?: return

        when (button) {
            button_dialpad -> (activity as NewCallActivity).openDialer()
            button_mute -> if (!call.state.isMuted) call.mute() else call.unmute()
            button_hold -> if (!call.state.isOnHold) call.hold() else call.unhold()
            button_transfer -> ""
            else -> ""
        }

        render()
    }

    private fun handleAudioSourceSelected(source: Routes) {
        when (source) {
            Routes.BLUETOOTH -> voip?.audio?.routeAudioViaBluetooth()
            Routes.SPEAKER -> voip?.audio?.routeAudioViaSpeaker()
            else -> voip?.audio?.routeAudioViaEarpiece()
        }

        render()
    }

    override fun render() {
        val voip = voip ?: return
        val call = voip.getCurrentCall() ?: return

        val buttons = arrayOf(button_hold, button_mute, button_transfer, button_audio_source, button_dialpad)
        val disabledDuringDialling = arrayOf(button_hold, button_mute, button_transfer)

        buttons.forEach { it.enable() }
        disabledDuringDialling.forEach { it.enable(call.state.telephonyState == State.TelephonyState.CONNECTED) }

        button_mute.activate(call.state.isMuted)
        button_hold.activate(call.state.isOnHold)
        button_audio_source.apply {
            audio = voip.audio
            updateBasedOnAudioRouter()
        }
    }
}
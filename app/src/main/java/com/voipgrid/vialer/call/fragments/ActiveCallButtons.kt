package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R
import com.voipgrid.vialer.audio.Routes
import com.voipgrid.vialer.call.AudioSourceButton
import com.voipgrid.vialer.call.CallActionButton
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.voip.core.call.State
import kotlinx.android.synthetic.main.fragment_call_active_buttons.*

class ActiveCallButtons : VoipAwareFragment() {

    private lateinit var buttons: Array<CallActionButton>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_active_buttons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttons = arrayOf(button_hold, button_mute, button_transfer, button_audio_source, button_dialpad)

        buttons.filter { it !is AudioSourceButton }.forEach { button ->
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
            button_transfer -> if (voip.isTransferring()) {
                button_transfer.activate()
                (activity as NewCallActivity).mergeTransfer()
            } else (activity as NewCallActivity).openTransferSelector()
            else -> {}
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
        val call = voip.getCurrentCall()

        if (call == null) {
            buttons.forEach { it.disable() }
            button_hangup.disable()
            return
        }

        val disabledDuringDialling = arrayOf(button_hold, button_mute, button_transfer)

        when (call.state.telephonyState) {
            State.TelephonyState.OUTGOING_CALLING -> buttons.forEach { it.enable(!disabledDuringDialling.contains(it)) }
            State.TelephonyState.CONNECTED -> buttons.forEach { it.enable() }
            else -> buttons.forEach { it.disable() }
        }

        if (voip.isTransferring()) {
            button_transfer.swapIconAndText(R.drawable.ic_call_merge, R.string.transfer_connect)
        } else {
            button_transfer.resetIconAndText()
        }

        button_mute.activate(call.state.isMuted)
        button_hold.activate(call.state.isOnHold)
        button_audio_source.apply {
            audio = voip.audio
            updateBasedOnAudioRouter()
        }
    }
}
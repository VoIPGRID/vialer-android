package com.voipgrid.vialer.sip

import android.telecom.CallAudioState
import android.telecom.Connection

class VialerConnection : Connection() {

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
    }

    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
    }

    override fun onHold() {
        super.onHold()
    }

    override fun onUnhold() {
        super.onUnhold()
    }

    override fun onAnswer() {


    }

    override fun onReject() {
        super.onReject()
    }

    override fun onDisconnect() {
        super.onDisconnect()
    }
}
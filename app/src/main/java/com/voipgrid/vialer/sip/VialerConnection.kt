package com.voipgrid.vialer.sip

import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.notifications.call.IncomingCallNotification

class VialerConnection : Connection() {

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
    }

    override fun onShowIncomingCallUi() {
        voip.incomingCallAlerts.start()
        IncomingCallNotification(voip.currentCall.phoneNumber, voip.currentCall.callerId).build()
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
        VialerApplication.get().sendBroadcast(Intent("VialerConnection"))
    }

    override fun onHold() {
        voip.currentCall.putOnHold()
    }

    override fun onUnhold() {
        voip.currentCall.takeOffHold()
        setActive()
    }

    override fun onAnswer() {
        voip.currentCall.answer()
        setActive()
    }

    override fun onReject() {
        voip.currentCall.decline()
        destroy()
    }

    override fun onDisconnect() {
        voip.currentCall.hangup(true)
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onSilence() {
        voip.incomingCallAlerts.stop()
    }

    companion object {
        lateinit var voip: SipService
    }
}
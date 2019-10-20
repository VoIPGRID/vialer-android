package com.voipgrid.vialer.voip.providers.pjsip.core

import android.util.Log
import com.voipgrid.vialer.voip.core.VoipListener
import com.voipgrid.vialer.voip.core.call.Call
import com.voipgrid.vialer.voip.core.call.Metadata
import com.voipgrid.vialer.voip.core.call.State
import com.voipgrid.vialer.voip.providers.pjsip.core.PjsipCall.MicrophoneStatus.*
import org.pjsip.pjsua2.*
import org.pjsip.pjsua2.pjsip_inv_state.*

abstract class PjsipCall : org.pjsip.pjsua2.Call, Call {

    private val listener: VoipListener

    override val state = State()
    final override val metaData: Metadata

    constructor(account: Account, listener: VoipListener, thirdParty: ThirdParty, direction: Call.Direction) : super(account) {
        this.listener = listener
        this.metaData = Metadata(thirdParty.number, thirdParty.name, direction)
    }

    constructor(account: Account, callId: Int, listener: VoipListener, thirdParty: ThirdParty, direction: Call.Direction) : super(account, callId) {
        this.listener = listener
        this.metaData = Metadata(thirdParty.number, thirdParty.name, direction)
    }

    override fun getDuration(unit: Call.DurationUnit): Int = when(unit) {
        Call.DurationUnit.SECONDS -> info.connectDuration.sec
        Call.DurationUnit.MILLISECONDS -> (info.connectDuration.sec * 1000) + info.connectDuration.msec
    }

    override fun answer() {
        Log.e("TEST123", "IN SIP CALL ANSWERING")
        super.answer(param(pjsip_status_code.PJSIP_SC_ACCEPTED))
    }

    override fun decline() {
        super.hangup(param(pjsip_status_code.PJSIP_SC_BUSY_HERE))
    }

    override fun hangup() = super.hangup(param(pjsip_status_code.PJSIP_SC_DECLINE))

    override fun hold() {
        super.setHold(CallOpParam(true))
        state.isOnHold = true
    }

    override fun unhold() {
        super.reinvite(CallOpParam(true).apply {
            opt.flag = pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue().toLong()
        })
        state.isOnHold = false
    }

    internal fun makeCall(dst_uri: String) = super.makeCall(dst_uri, param(pjsip_status_code.PJSIP_SC_RINGING))

    protected fun param(code: pjsip_status_code) = CallOpParam().apply {
        statusCode = code
    }

    override fun onCallTsxState(prm: OnCallTsxStateParam?) {
        super.onCallTsxState(prm)
        Log.e("TEST123", "tsx state: ${prm.toString()}")
    }

    override fun onCallState(prm: OnCallStateParam?) {
        super.onCallState(prm)
        updateState()
        updateMetadata()
    }

    private fun updateMetadata() {

    }

    override fun onCallMediaState(prm: OnCallMediaStateParam?) {
        super.onCallMediaState(prm)
        Log.e("TEST123", "onCallMediaStateParam: ${prm.toString()}")
    }

    private fun updateState() {
        state.telephonyState = when(info.state) {
            PJSIP_INV_STATE_NULL -> State.TelephonyState.INITIALIZING
            PJSIP_INV_STATE_CALLING -> State.TelephonyState.OUTGOING_CALLING
            PJSIP_INV_STATE_INCOMING,
            PJSIP_INV_STATE_CONNECTING,
            PJSIP_INV_STATE_EARLY -> if(metaData.direction == Call.Direction.INCOMING) State.TelephonyState.INCOMING_RINGING else State.TelephonyState.OUTGOING_CALLING
            PJSIP_INV_STATE_CONFIRMED -> State.TelephonyState.CONNECTED
            PJSIP_INV_STATE_DISCONNECTED -> State.TelephonyState.DISCONNECTED
            else -> state.telephonyState
        }

        Log.e("TEST123", "Update state ${info.state} broadcasting ${state.telephonyState}")
        this.listener.onCallStateUpdate(this, state)
    }

    override fun mute() {
        Log.e("TEST123", "muting..")
        adjustMicrophoneVolume(MUTED)
        state.isMuted = true
    }

    override fun unmute() {
        adjustMicrophoneVolume(UNMUTED)
        state.isMuted = false
    }

    override fun sendDtmf(digit: String) {
        state.dtmfDialed = state.dtmfDialed.plus(digit)
        dialDtmf(digit)
    }

    private fun adjustMicrophoneVolume(microphoneStatus: MicrophoneStatus) {
        val callMediaInfoVector = this.info.media

        for (i in 0 until callMediaInfoVector.size()) {
            val callMediaInfo = callMediaInfoVector.get(i.toInt())

            if (callMediaInfo.type != pjmedia_type.PJMEDIA_TYPE_AUDIO) continue

            AudioMedia.typecastFromMedia(this.getMedia(i))?.adjustRxLevel(when (microphoneStatus) {
                MUTED -> 0
                UNMUTED -> 2
            }.toFloat())
        }
    }

    enum class MicrophoneStatus {
        MUTED, UNMUTED
    }
}
package com.voipgrid.vialer.voip.providers.pjsip.core

import android.util.Log
import com.voipgrid.vialer.voip.core.call.Call
import com.voipgrid.vialer.voip.core.call.Metadata
import com.voipgrid.vialer.voip.core.call.State
import org.pjsip.pjsua2.*
import org.pjsip.pjsua2.pjsip_inv_state.*

class PjsipCall : org.pjsip.pjsua2.Call, Call {

    override val metaData = Metadata()
    override val state = State()

    override val direction = Call.Direction.OUTGOING

    constructor(account: Account) : super(account)

    constructor(account: Account, callId: Int) : super(account, callId)

    override fun getDuration(unit: Call.DurationUnit): Int = when(unit) {
        Call.DurationUnit.SECONDS -> info.connectDuration.sec
        Call.DurationUnit.MILLISECONDS -> (info.connectDuration.sec * 1000) + info.connectDuration.msec
    }

    override fun answer() {
        super.answer(param(pjsip_status_code.PJSIP_SC_ACCEPTED))
    }

    override fun decline() {
        super.hangup(param(pjsip_status_code.PJSIP_SC_BUSY_HERE))
    }

    override fun hangup() {
        super.hangup(param(pjsip_status_code.PJSIP_SC_DECLINE))
    }

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

    private fun param(code: pjsip_status_code) = CallOpParam().apply {
        statusCode = code
    }

    override fun onCallTsxState(prm: OnCallTsxStateParam?) {
        super.onCallTsxState(prm)
        Log.e("TEST123", "tsx state")
    }

    override fun onCallState(prm: OnCallStateParam?) {
        super.onCallState(prm)
        metaData.callerId = "A test caller id"
        metaData.number = "08001696000"
        Log.e("TEST123", "Call state: ${info.state}")
        state.telephonyState = when(info.state) {
            PJSIP_INV_STATE_NULL -> State.TelephonyState.INITIALIZING
            PJSIP_INV_STATE_CALLING -> State.TelephonyState.CALLING
            PJSIP_INV_STATE_INCOMING -> State.TelephonyState.RINGING
            PJSIP_INV_STATE_CONFIRMED -> State.TelephonyState.CONNECTED
            else -> state.telephonyState
        }
    }

    fun answerAsRinging() {
        Log.e("TEST123", "Answering as ringing..")
        try {
        super.answer(param(pjsip_status_code.PJSIP_SC_RINGING))

        } catch (e: Exception) {
            Log.e("TEST123", "Failed to answer ans ringing", e)
        }

        Log.e("TEST123", "Answered as ringing..")
    }
}
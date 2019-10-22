package com.voipgrid.vialer.voip.providers.pjsip.core

import com.voipgrid.vialer.voip.core.VoipListener
import com.voipgrid.vialer.voip.core.call.Call
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.pjsip_status_code

internal class IncomingCall(account: Account, callId: Int, listener: VoipListener, thirdParty: ThirdParty) : PjsipCall(account, callId, listener, thirdParty, Call.Direction.INCOMING) {

    /**
     * Acknowledge the incoming call so ringing will begin.
     *
     */
    fun acknowledge() = super.answer(param(pjsip_status_code.PJSIP_SC_RINGING))
}
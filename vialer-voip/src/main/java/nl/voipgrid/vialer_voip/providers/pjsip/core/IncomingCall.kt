package nl.voipgrid.vialer_voip.providers.pjsip.core

import nl.voipgrid.vialer_voip.core.VoipListener
import nl.voipgrid.vialer_voip.core.call.Call
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.pjsip_status_code

internal class IncomingCall(account: Account, callId: Int, listener: VoipListener, thirdParty: ThirdParty) : PjsipCall(account, callId, listener, thirdParty, Call.Direction.INCOMING) {

    /**
     * Acknowledge the incoming call so ringing will begin.
     *
     */
    fun acknowledge() = super.answer(param(pjsip_status_code.PJSIP_SC_RINGING))
}
package com.voipgrid.vialer.voip.providers.pjsip.core

import com.voipgrid.vialer.voip.core.VoipListener
import com.voipgrid.vialer.voip.core.call.Call

internal class OutgoingCall(account: org.pjsip.pjsua2.Account, listener: VoipListener, thirdParty: ThirdParty) : PjsipCall(account, listener, thirdParty, Call.Direction.OUTGOING) {

}
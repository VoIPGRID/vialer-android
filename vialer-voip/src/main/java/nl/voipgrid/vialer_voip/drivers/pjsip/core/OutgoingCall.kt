package nl.voipgrid.vialer_voip.drivers.pjsip.core

import nl.voipgrid.vialer_voip.core.VoipListener
import nl.voipgrid.vialer_voip.core.call.Call

internal class OutgoingCall(account: org.pjsip.pjsua2.Account, listener: VoipListener, thirdParty: ThirdParty) : PjsipCall(account, listener, thirdParty, Call.Direction.OUTGOING) {

}
package com.voipgrid.vialer.sip.core

import com.voipgrid.vialer.sip.SipCall

interface CallListener {

    fun onTelephonyStateChange(call: SipCall, state: SipCall.TelephonyState)

    fun onCallMissed(call: SipCall)

}
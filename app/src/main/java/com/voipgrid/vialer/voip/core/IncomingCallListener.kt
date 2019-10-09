package com.voipgrid.vialer.voip.core

import com.voipgrid.vialer.voip.core.call.Call

interface IncomingCallListener {

    fun onIncomingCall(call: Call)
}
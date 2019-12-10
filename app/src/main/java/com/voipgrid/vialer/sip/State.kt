package com.voipgrid.vialer.sip

class State {

    var telephonyState = SipCall.TelephonyState.INITIALIZING
    var isOnHold = false
    var wasUserHangup = false
    var isIpChangeInProgress = false
    var isMuted = false
}
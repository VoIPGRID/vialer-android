package com.voipgrid.vialer.phonelib

import android.text.format.DateUtils
import com.voipgrid.vialer.sip.SipConstants.*
import org.openvoipalliance.phonelib.model.CallState.*
import org.openvoipalliance.phonelib.model.Session

fun Session.isRinging() = when (state) {
    IncomingReceived -> true
    else -> false
}

fun Session.isConnected() = when (state) {
    IncomingReceived, OutgoingInit, OutgoingProgress, OutgoingRinging,
    OutgoingEarlyMedia, Connected, StreamsRunning, Pausing, Paused,
    Resuming, Referred, PausedByRemote, CallUpdatedByRemote,
    CallIncomingEarlyMedia, CallUpdating -> true
    else -> false
}

fun Session.isOnHold() = when (state) {
    Paused -> true
    else -> false
}

val Session.prettyCallDuration
    get() = DateUtils.formatElapsedTime(duration.toLong())

val Session.isIncoming
    get() = true

val Session.isOutgoing
    get() = false

val Session.callId
    get() = "spindle-12345-test"

fun Session.getCallDurationInMilliseconds() = duration * 1000

val Session.legacyState
    get() = when(state) {
        Idle, IncomingReceived, OutgoingInit, OutgoingProgress, OutgoingRinging, OutgoingEarlyMedia , Unknown-> CALL_INVALID_STATE
        Connected, StreamsRunning, Pausing, Paused, Resuming, Referred, PausedByRemote, CallUpdatedByRemote, CallIncomingEarlyMedia,
        CallUpdating, CallEarlyUpdatedByRemote, CallEarlyUpdating -> CALL_CONNECTED_MESSAGE
        Error, CallReleased, CallEnd -> CALL_DISCONNECTED_MESSAGE
    }
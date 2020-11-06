package com.voipgrid.vialer.phonelib

import android.text.format.DateUtils
import com.voipgrid.vialer.sip.SipConstants.*
import org.openvoipalliance.phonelib.model.CallState.*
import org.openvoipalliance.phonelib.model.Direction
import org.openvoipalliance.phonelib.model.Call

fun Call.isRinging() = when (state) {
    IncomingReceived -> true
    else -> false
}

fun Call.isConnected() = when (state) {
    IncomingReceived, OutgoingInit, OutgoingProgress, OutgoingRinging,
    OutgoingEarlyMedia, Connected, StreamsRunning, Pausing, Paused,
    Resuming, Referred, PausedByRemote, CallUpdatedByRemote,
    CallIncomingEarlyMedia, CallUpdating -> true
    else -> false
}

fun Call.isOnHold() = when (state) {
    Paused -> true
    else -> false
}

val Call.prettyCallDuration
    get() = DateUtils.formatElapsedTime(duration.toLong())

val Call.isIncoming
    get() = direction == Direction.INCOMING

val Call.isOutgoing
    get() = direction == Direction.OUTGOING

fun Call.getCallDurationInMilliseconds() = duration * 1000

val Call.legacyState
    get() = when(state) {
        Idle, IncomingReceived, OutgoingInit, OutgoingProgress, OutgoingRinging, OutgoingEarlyMedia , Unknown-> CALL_INVALID_STATE
        Connected, StreamsRunning, Pausing, Paused, Resuming, Referred, PausedByRemote, CallUpdatedByRemote, CallIncomingEarlyMedia,
        CallUpdating, CallEarlyUpdatedByRemote, CallEarlyUpdating -> CALL_CONNECTED_MESSAGE
        Error, CallReleased, CallEnd -> CALL_DISCONNECTED_MESSAGE
    }
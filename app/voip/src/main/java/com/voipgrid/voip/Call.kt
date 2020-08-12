package com.voipgrid.voip

import com.voipgrid.voip.android.Connection
import com.voipgrid.voip.ui.Display
import nl.spindle.phonelib.model.CallState.*
import nl.spindle.phonelib.model.Session
import java.util.*

class Call(val session: Session, val direction: Direction) {

    val display
        get() = Display(session.getPhoneNumber, session.linphoneCall.remoteAddress.displayName ?: "", session.getDuration)

    val uuid = UUID.randomUUID()

    val duration: Int
        get() = session.getDuration

    lateinit var connection: Connection

    enum class Direction {
        OUTBOUND, INBOUND
    }

    enum class State {
        INITIALIZING, RINGING, CONNECTED, HELD_BY_LOCAL, HELD_BY_REMOTE, ENDED, ERROR
    }

    val state: State
        get() {
            return when (session.getState) {
                Idle, IncomingReceived, OutgoingInit, OutgoingProgress, OutgoingEarlyMedia -> State.INITIALIZING
                OutgoingRinging -> State.RINGING

                Connected, StreamsRunning, Referred, CallUpdatedByRemote, CallIncomingEarlyMedia, CallUpdating, CallEarlyUpdatedByRemote,
                CallEarlyUpdating -> State.CONNECTED

                Pausing, Paused, Resuming -> State.HELD_BY_LOCAL

                Error, CallReleased -> State.ERROR

                CallEnd -> State.ENDED

                PausedByRemote -> State.HELD_BY_REMOTE

                Unknown -> State.ENDED
            }
        }

    val isOnHold: Boolean
        get() = state == State.HELD_BY_LOCAL


}
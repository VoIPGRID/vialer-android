package com.voipgrid.voip

import nl.spindle.phonelib.model.Session
import java.util.*

class Call(val session: Session, val direction: Direction) {

    val uuid = UUID.randomUUID()

    val duration: Int
        get() = session.getDuration

    enum class Direction {
        OUTBOUND, INBOUND
    }
}
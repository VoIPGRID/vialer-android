package com.voipgrid.vialer.voip.core.call

interface Call {

    val metaData: Metadata

    val state: State

    fun getDuration(unit: DurationUnit = DurationUnit.SECONDS): Int

    fun answer()

    fun decline()

    fun hangup()

    fun hold()

    fun unhold()

    fun mute()

    fun unmute()

    fun sendDtmf(digit: String)

    enum class DurationUnit {
        SECONDS, MILLISECONDS
    }

    enum class Direction {
        INCOMING, OUTGOING
    }
}
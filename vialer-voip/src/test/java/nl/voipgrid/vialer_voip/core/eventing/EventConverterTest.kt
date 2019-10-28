package nl.voipgrid.vialer_voip.core.eventing

import com.squareup.moshi.Moshi
import nl.voipgrid.vialer_voip.core.call.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventConverterTest {

    private val moshi = Moshi.Builder().build()
    private val converter = EventConverter(moshi)

    @Test
    fun `it can convert an event name and payload to an event object`() {
        val event = CallStateDidChange(State.TelephonyState.CONNECTED, State.TelephonyState.INCOMING_RINGING)

        val convertedEvent = converter.convertToEvent(event.javaClass.name, moshi.adapter(event.javaClass).toJson(event))

        assertTrue(convertedEvent is CallStateDidChange)

        val callStateDidChangeEvent = convertedEvent as CallStateDidChange

        assertEquals(State.TelephonyState.INCOMING_RINGING, callStateDidChangeEvent.previousState)
        assertEquals(State.TelephonyState.CONNECTED, callStateDidChangeEvent.state)
    }

    @Test
    fun `it can convert an event name and payload to an event object that has no arguments`() {
        val event = IncomingCallStartedRinging

        assertTrue(converter.convertToEvent(event.javaClass.name, moshi.adapter(event.javaClass).toJson(event)) is IncomingCallStartedRinging)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `it will throw an exception if the event cannot be converted`() {
        converter.convertToEvent("not a name", "not a payload")
    }

    @Test
    fun `it will convert event object to name and payload and back again`() {
        val event = CallStateDidChange(State.TelephonyState.INITIALIZING, State.TelephonyState.OUTGOING_CALLING)
        val serializedEvent = converter.convertFromEvent(event)
        val convertedEvent = converter.convertToEvent(serializedEvent.name, serializedEvent.payload)
        assertTrue(convertedEvent is CallStateDidChange)
        val callStateDidChangeEvent = convertedEvent as CallStateDidChange
        assertEquals(State.TelephonyState.OUTGOING_CALLING, callStateDidChangeEvent.previousState)
        assertEquals(State.TelephonyState.INITIALIZING, callStateDidChangeEvent.state)
    }
}

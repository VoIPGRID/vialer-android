package nl.voipgrid.vialer_voip.core.eventing

import com.squareup.moshi.Moshi

class EventConverter(private val json: Moshi) {

    /**
     * Convert an event name and payload to an actual event class.
     *
     */
    fun convertToEvent(name: String, payload: String) : Event {
        try {
            when (val event = json.adapter(Class.forName(name)).fromJson(payload)) {
                is Event -> return event
                else -> throw IllegalArgumentException("Unable to convert event $name with payload $payload")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to convert event $name with payload $payload")
        }
    }

    /**
     * Convert an event object into a serialized form of the event.
     *
     */
    fun convertFromEvent(event: Event): SerializedEvent = SerializedEvent(
            event.javaClass.name,
            json.adapter(event.javaClass).toJson(event)
    )

    data class SerializedEvent(val name: String, val payload: String)
}
package nl.voipgrid.vialer_voip.core

import nl.voipgrid.vialer_voip.core.call.Call
import java.lang.Exception

class CallStack : ArrayList<Call>() {
    val active: Call? get() = try { last() } catch(e: Exception) { null }
    val original: Call? get() = try { first() } catch(e: Exception) { null }
}
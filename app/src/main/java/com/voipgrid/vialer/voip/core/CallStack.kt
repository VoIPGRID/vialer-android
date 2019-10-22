package com.voipgrid.vialer.voip.core

import com.voipgrid.vialer.voip.core.call.Call
import java.lang.Exception

class CallStack : ArrayList<Call>() {
    val active: Call? get() = try { last() } catch(e: Exception) { null }
    val original: Call? get() = try { first() } catch(e: Exception) { null }
}
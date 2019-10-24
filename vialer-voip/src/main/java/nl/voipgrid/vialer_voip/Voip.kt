package nl.voipgrid.vialer_voip

import nl.voipgrid.vialer_voip.core.Configuration
import nl.voipgrid.vialer_voip.core.Credentials
import nl.voipgrid.vialer_voip.core.call.Call
import org.koin.core.context.loadKoinModules

object Voip {

    fun init() = loadKoinModules(voipModule)

    lateinit var configuration: () -> Configuration
    lateinit var credentials: () -> Credentials
    lateinit var notification: (call: Call?) -> Unit
}
package nl.voipgrid.vialer_voip.drivers.pjsip.initialization.config.endpoint

import nl.voipgrid.vialer_voip.core.Configuration
import org.pjsip.pjsua2.EpConfig

internal interface EndpointConfigurator {

    fun configure(config: Configuration, endpointConfig: EpConfig)
}
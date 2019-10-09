package com.voipgrid.vialer.voip.providers.pjsip.initialization.config.endpoint

import com.voipgrid.vialer.voip.core.Configuration
import org.pjsip.pjsua2.EpConfig

interface EndpointConfigurator {

    fun configure(config: Configuration, endpointConfig: EpConfig)
}
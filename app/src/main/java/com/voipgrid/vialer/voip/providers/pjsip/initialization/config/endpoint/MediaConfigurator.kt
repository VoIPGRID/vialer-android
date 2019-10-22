package com.voipgrid.vialer.voip.providers.pjsip.initialization.config.endpoint

import com.voipgrid.vialer.voip.core.Configuration
import org.pjsip.pjsua2.EpConfig

internal class MediaConfigurator : EndpointConfigurator {

    override fun configure(config: Configuration, endpointConfig: EpConfig) {
        val mediaConfig = endpointConfig.medConfig
        mediaConfig.ecOptions = config.echoCancellation
        mediaConfig.ecTailLen = config.echoCancellationTailLength
        endpointConfig.medConfig = mediaConfig
    }
}
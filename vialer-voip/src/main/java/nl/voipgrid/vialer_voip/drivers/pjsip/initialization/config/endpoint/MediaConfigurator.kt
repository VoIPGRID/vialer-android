package nl.voipgrid.vialer_voip.drivers.pjsip.initialization.config.endpoint

import nl.voipgrid.vialer_voip.core.Configuration
import org.pjsip.pjsua2.EpConfig

internal class MediaConfigurator : EndpointConfigurator {

    override fun configure(config: Configuration, endpointConfig: EpConfig) {
        val mediaConfig = endpointConfig.medConfig
        mediaConfig.ecOptions = config.echoCancellation
        mediaConfig.ecTailLen = config.echoCancellationTailLength
        endpointConfig.medConfig = mediaConfig
    }
}
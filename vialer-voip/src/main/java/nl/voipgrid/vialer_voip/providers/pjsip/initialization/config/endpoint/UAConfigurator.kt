package nl.voipgrid.vialer_voip.providers.pjsip.initialization.config.endpoint

import nl.voipgrid.vialer_voip.core.Configuration
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.StringVector

internal class UAConfigurator : EndpointConfigurator {

    private val stunHosts = arrayOf("stun.l.google.com:19302")

    override fun configure(config: Configuration, endpointConfig: EpConfig) {
        val uaConfig = endpointConfig.uaConfig
        uaConfig.userAgent = config.userAgent
        uaConfig.mainThreadOnly = true

        if (config.stun) {
            val stun = StringVector()

            stunHosts.forEach { stun.add(it) }

            uaConfig.stunServer = stun
        }
        endpointConfig.uaConfig = uaConfig
    }
}
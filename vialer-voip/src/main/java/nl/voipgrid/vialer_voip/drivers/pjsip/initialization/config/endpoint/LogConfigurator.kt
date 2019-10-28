package nl.voipgrid.vialer_voip.drivers.pjsip.initialization.config.endpoint

import nl.voipgrid.vialer_voip.core.Configuration
import nl.voipgrid.vialer_voip.drivers.pjsip.PjsipDriver
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.pj_log_decoration

internal class LogConfigurator : EndpointConfigurator {

    override fun configure(config: Configuration, endpointConfig: EpConfig) {
        endpointConfig.logConfig.apply {
            level = 10
            consoleLevel = 4
//            writer = PjsipDriver.logWriter
            decor = decor and (pj_log_decoration.PJ_LOG_HAS_CR.swigValue() or pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()).toLong()
        }
    }
}
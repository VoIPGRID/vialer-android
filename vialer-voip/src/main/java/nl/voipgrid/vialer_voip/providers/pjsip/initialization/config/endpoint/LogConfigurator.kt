package nl.voipgrid.vialer_voip.providers.pjsip.initialization.config.endpoint

import com.voipgrid.vialer.sip.SipLogWriter
import nl.voipgrid.vialer_voip.core.Configuration
import nl.voipgrid.vialer_voip.providers.pjsip.PjsipProvider
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.pj_log_decoration

internal class LogConfigurator : EndpointConfigurator {

    override fun configure(config: Configuration, endpointConfig: EpConfig) {
        endpointConfig.logConfig.apply {
            level = 10
            consoleLevel = 4
            writer = PjsipProvider.logWriter
            decor = decor and (pj_log_decoration.PJ_LOG_HAS_CR.swigValue() or pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()).toLong()
        }
    }
}
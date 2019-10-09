package com.voipgrid.vialer.voip.providers.pjsip.initialization.config

import android.util.Log
import com.voipgrid.vialer.voip.core.Configuration
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.pj_constants_.PJ_TRUE
import org.pjsip.pjsua2.pjmedia_srtp_use
import org.pjsip.pjsua2.pjsua_call_flag.*

class AccountConfigurator {

    fun configure(configuration: Configuration): AccountConfig {
        val credentialInformation = createCredentialInformation(configuration)

        return AccountConfig().apply {
            idUri = configuration.host.withAccount(configuration.accountId)
            regConfig.registrarUri = configuration.host.withTransport(configuration.transport)
            sipConfig.authCreds.add(credentialInformation)
            sipConfig.proxies.add(configuration.host.withTransport(configuration.transport))
            ipChangeConfig.shutdownTp = false
            ipChangeConfig.hangupCalls = false
            ipChangeConfig.reinviteFlags = (PJSUA_CALL_UPDATE_CONTACT.swigValue() or PJSUA_CALL_REINIT_MEDIA.swigValue() or PJSUA_CALL_UPDATE_VIA.swigValue()).toLong()
            natConfig.contactRewriteUse = PJ_TRUE.swigValue()
            natConfig.contactRewriteMethod = 4

            if (configuration.transport == Configuration.Transport.TLS) {
                Log.e("TEST123", "SECURE!!!")
                mediaConfig.srtpSecureSignaling = 1
                mediaConfig.srtpUse = pjmedia_srtp_use.PJMEDIA_SRTP_MANDATORY
            }
        }
    }

    private fun createCredentialInformation(configuration: Configuration) = AuthCredInfo(
            configuration.scheme,
            configuration.realm,
            configuration.accountId,
            0,
            configuration.password
    )
}
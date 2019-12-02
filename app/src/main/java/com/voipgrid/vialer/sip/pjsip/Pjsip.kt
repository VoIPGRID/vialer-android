package com.voipgrid.vialer.sip.pjsip

import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.pjsip.PjsipConfigurator.LibraryInitFailedException
import org.pjsip.pjsua2.*

class Pjsip(private val pjsipConfigurator: PjsipConfigurator, private val phoneAccount: PhoneAccount) {

    private val logger = Logger(this)

    lateinit var endpoint: VialerEndpoint
        private set

    private lateinit var sipService: SipService

    lateinit var account: SipAccount
        private set

    /**
     * Begin the process of actually starting the SIP library so we can
     * start/receive calls.
     *
     */
    fun init(sipService: SipService) {
        this.sipService = sipService

        try {
            logger.i("Attempting to load sip lib")
            loadPjsip()
            endpoint = pjsipConfigurator.initializeEndpoint(VialerEndpoint())
            account = SipAccount(pjsipConfigurator.createAccountConfig(phoneAccount))
        } catch (e: java.lang.Exception) {
            logger.e("Failed to load pjsip, stopping the service")
            sipService.stopSelf()
        }
    }

    fun destroy() {
        pjsipConfigurator.cleanUp()
        endpoint.libDestroy()
        endpoint.delete()
    }

    @Throws(LibraryInitFailedException::class)
    private fun loadPjsip() {
        logger.d("Loading PJSIP")
        System.loadLibrary("pjsua2")
    }

    /**
     * The pjsip account.
     *
     */
    inner class SipAccount(accountConfig: AccountConfig) : Account() {

        init {
            create(accountConfig)
        }

        override fun onIncomingCall(incomingCallParam: OnIncomingCallParam) {
            sipService.onIncomingCall(incomingCallParam, this)
        }

        override fun onRegState(regStateParam: OnRegStateParam) {
            try {
                if (info.regIsActive) {
                    sipService.onRegister()
                }
            } catch (exception: Exception) {
            }
        }
    }

    inner class VialerEndpoint : Endpoint() {

        override fun onTransportState(prm: OnTransportStateParam) {
            super.onTransportState(prm)

            if (prm.state == pjsip_transport_state.PJSIP_TP_STATE_CONNECTED) {
                logger.i("There has been a new transport created. Reinivite the calls to keep the call going.")
                sipService.reinvite()
            }
        }
    }
}
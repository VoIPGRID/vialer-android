package com.voipgrid.vialer.sip.pjsip

import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipService
import org.pjsip.pjsua2.*

class Pjsip(private val pjsipConfigurator: PjsipConfigurator, private val phoneAccount: PhoneAccount) {

    private val logger = Logger(this)

    var endpoint: VialerEndpoint? = null
        private set

    lateinit var account: SipAccount
        private set

    private lateinit var sip: SipService

    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    /**
     * Initialize the pjsip library and prepare it to receive or make calls.
     *
     */
    fun init(sipService: SipService) {
        this.sip = sipService

        try {
            if (endpoint != null) return

            endpoint = pjsipConfigurator.initializeEndpoint(VialerEndpoint())
            account = SipAccount(pjsipConfigurator.createAccountConfig(phoneAccount))
        } catch (e: java.lang.Exception) {
            logger.e("Failed to load pjsip, stopping the service")
            sipService.stopSelf()
        }
    }

    /**
     * Destroy and clean up pjsip.
     *
     */
    fun destroy() {
        pjsipConfigurator.cleanUp()
        account.delete()
        endpoint?.apply {
            libDestroy()
            delete()
        }
        endpoint = null
    }

    inner class SipAccount(accountConfig: AccountConfig) : Account() {

        init {
            create(accountConfig)
        }

        override fun onIncomingCall(incomingCallParam: OnIncomingCallParam) {
            sip.handler.onIncomingCall(incomingCallParam, this)
        }

        override fun onRegState(regStateParam: OnRegStateParam) {
            try {
                if (info.regIsActive) {
                    sip.handler.onRegister()
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
                sip.actions.reinvite()
            }
        }
    }

    companion object {
        private const val LIBRARY_NAME = "pjsua2"
    }
}
package com.voipgrid.vialer.sip.pjsip

import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.core.SipConfig
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.core.Action
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.OnRegStateParam

class Pjsip(private val sipConfig: SipConfig, private val phoneAccount: PhoneAccount) {

    private val logger = Logger(this)

    /**
     * Begin the process of actually starting the SIP library so we can
     * start/receive calls.
     *
     */
    private fun init(sipService: SipService) {

        try {
            logger.i("Attempting to load sip lib")
            sipConfig.init(sipService, phoneAccount, intent != null && Action.HANDLE_INCOMING_CALL == intent.getAction())
            sipConfig.initLibrary()
        } catch (e: java.lang.Exception) {
            logger.e("Failed to load pjsip, stopping the service")
            sipService.stopSelf()
        }
    }

    private fun destroy() {
        sipConfig.cleanUp()
    }

    inner class SipAccount private constructor(accountConfig: AccountConfig) : Account() {
        override fun onIncomingCall(incomingCallParam: OnIncomingCallParam) {
            this@SipService.onIncomingCall(incomingCallParam, this)
        }

        override fun onRegState(regStateParam: OnRegStateParam) {
            try {
                if (info.regIsActive) {
                    getSipConfig().onAccountRegistered(this, regStateParam)
                }
            } catch (exception: Exception) {
            }
        }

        init {
            create(accountConfig)
        }
    }
}
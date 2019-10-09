package com.voipgrid.vialer.voip.providers.pjsip

import android.util.Log
import com.voipgrid.vialer.sip.SipLogWriter
import com.voipgrid.vialer.voip.core.Configuration
import com.voipgrid.vialer.voip.core.IncomingCallListener
import com.voipgrid.vialer.voip.core.VoipProvider
import com.voipgrid.vialer.voip.providers.pjsip.core.PjsipCall
import com.voipgrid.vialer.voip.providers.pjsip.core.PjsipEndpoint
import com.voipgrid.vialer.voip.providers.pjsip.initialization.Initializer
import com.voipgrid.vialer.voip.providers.pjsip.initialization.config.AccountConfigurator
import kotlinx.coroutines.*
import org.pjsip.pjsua2.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PjsipProvider : VoipProvider {

    private val initializer = Initializer()
    private var endpoint: PjsipEndpoint? = null
    private lateinit var configuration: Configuration
    private val accountConfiguration = AccountConfigurator()
    private var account: Account? = null
    private lateinit var listener: IncomingCallListener

    /**
     * This is a continuation so we can convert our account registration
     * callbacks into a coroutine.
     *
     */
    private var registerContinuation: Continuation<Account>? = null

    override fun initialize(configuration: Configuration, listener: IncomingCallListener) {
        this.configuration = configuration
        this.listener = listener
        if (endpoint == null) {
            endpoint = initializer.initialize(configuration)
        }
    }

    /**
     * Place a call to a phone number.
     *
     */
    override suspend fun call(number: String): PjsipCall = withContext(Dispatchers.IO) {
        register()

        val call = PjsipCall(account!!)

        call.makeCall(configuration.host.withAccount(number))

        return@withContext call
    }



    /**
     * Register the account that has been provided by the configuration.
     *
     */
    override suspend fun register() = withContext(Dispatchers.IO) {
        if (account == null || account?.info?.regIsActive == false) {
            suspendCoroutine<Account> {
                registerContinuation = it
                account = Account().apply {
                    Log.e("TEST123", "Attempting registration")
                    create(accountConfiguration.configure(configuration))
                }
            }
        }
    }

    override fun destroy() {
        endpoint?.let {
            it.libDestroy()
            it.delete()
        }
        account = null
        endpoint = null
    }

    /**
     * The pjsip account object, used to provide various callbacks about actions
     * taken by the library.
     *
     */
    private inner class Account : org.pjsip.pjsua2.Account() {
        override fun onRegStarted(prm: OnRegStartedParam?) {
            super.onRegStarted(prm)
            Log.e("TEST123", "onRegStarted")
        }

        override fun onRegState(prm: OnRegStateParam) {
            super.onRegState(prm)

            Log.e("TEST123", "onRegState: ${prm.reason} hasContinuation: " + (registerContinuation != null))
            when(info.regIsActive) {
                true -> registerContinuation?.resume(this)
            }
        }

        override fun onIncomingCall(prm: OnIncomingCallParam) {
            super.onIncomingCall(prm)
            Log.e("TEST123", "call id= ${prm.callId}")

                val call = PjsipCall(this@Account, 0)
                Log.e("TEST123", "reason:  ${Thread.currentThread().name}")


             call.answerAsRinging()


            listener.onIncomingCall(call)

            call.answer()
        }
    }

    companion object {
        lateinit var logWriter: SipLogWriter
    }
}
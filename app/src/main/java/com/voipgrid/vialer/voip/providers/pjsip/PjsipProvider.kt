package com.voipgrid.vialer.voip.providers.pjsip

import android.util.Log
import com.voipgrid.vialer.sip.SipLogWriter
import com.voipgrid.vialer.voip.core.CallListener
import com.voipgrid.vialer.voip.core.Configuration
import com.voipgrid.vialer.voip.core.VoipProvider
import com.voipgrid.vialer.voip.providers.pjsip.core.*
import com.voipgrid.vialer.voip.providers.pjsip.initialization.Initializer
import com.voipgrid.vialer.voip.providers.pjsip.initialization.config.AccountConfigurator
import com.voipgrid.vialer.voip.providers.pjsip.packets.Invite
import kotlinx.coroutines.*
import org.pjsip.pjsua2.*
import kotlin.Error
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PjsipProvider : VoipProvider {

    private val initializer = Initializer()
    private var endpoint: PjsipEndpoint? = null
    private lateinit var configuration: Configuration
    private val accountConfiguration = AccountConfigurator()
    private var account: Account? = null
    private lateinit var listener: CallListener

    /**
     * This is a continuation so we can convert our account registration
     * callbacks into a coroutine.
     *
     */
    private var registerContinuation: Continuation<Account>? = null

    override fun initialize(configuration: Configuration, listener: CallListener) {
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

        val account = account ?: throw Exception("No account to place a call with")

        val call = OutgoingCall(account, listener, ThirdParty(number = number, name = ""))

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

            val continuation = registerContinuation
            Log.e("TEST123", "onRegState: ${prm.reason} hasContinuation: " + (registerContinuation != null))
            registerContinuation = null
            when(info.regIsActive) {
                true -> continuation?.resume(this)
            }
        }

        override fun onIncomingCall(prm: OnIncomingCallParam) {
            super.onIncomingCall(prm)
            try {
                val call = IncomingCall(this@Account, 0, listener, Invite(prm.rdata.wholeMsg).findThirdParty())
                Log.e("TEST123", "reason:  ${Thread.currentThread().name}")

                call.acknowledge()
                listener.onIncomingCall(call)

            } catch (e: Throwable) {
                Log.e("TEST123", " ERRROR " , e)
            }

        }
    }

    companion object {
        lateinit var logWriter: SipLogWriter
    }
}
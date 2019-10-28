package nl.voipgrid.vialer_voip.drivers.pjsip

import nl.voipgrid.vialer_voip.core.VoipListener
import nl.voipgrid.vialer_voip.core.Configuration
import nl.voipgrid.vialer_voip.core.Credentials
import nl.voipgrid.vialer_voip.core.VoipDriver
import nl.voipgrid.vialer_voip.core.call.Call
import nl.voipgrid.vialer_voip.drivers.pjsip.core.IncomingCall
import nl.voipgrid.vialer_voip.drivers.pjsip.core.OutgoingCall
import nl.voipgrid.vialer_voip.drivers.pjsip.core.PjsipCall
import nl.voipgrid.vialer_voip.drivers.pjsip.core.PjsipEndpoint
import nl.voipgrid.vialer_voip.drivers.pjsip.core.ThirdParty
import nl.voipgrid.vialer_voip.drivers.pjsip.initialization.Initializer
import nl.voipgrid.vialer_voip.drivers.pjsip.initialization.config.AccountConfigurator
import nl.voipgrid.vialer_voip.drivers.pjsip.packets.Invite
import org.pjsip.pjsua2.*

internal class PjsipDriver : VoipDriver {

    private val initializer = Initializer()
    private var endpoint: PjsipEndpoint? = null
    private lateinit var configuration: Configuration
    private val accountConfiguration = AccountConfigurator()
    private var account: Account? = null
    private lateinit var listener: VoipListener

    override fun initialize(configuration: Configuration, listener: VoipListener) {
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
    override fun call(number: String): PjsipCall {
        val account = account ?: throw Exception("No account to place a call with")

        val call = OutgoingCall(account, listener, ThirdParty(number = number, name = ""))

        call.makeCall(configuration.host.withAccount(number))

        return call
    }

    /**
     * Register the account that has been provided by the configuration.
     *
     */
    override fun register(credentials: Credentials) {
        if (account == null || account?.info?.regIsActive == false) {
            account = Account().apply {
                create(accountConfiguration.configure(configuration, credentials))
            }
        } else {
            listener.onVoipProviderRegistered()
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

    override fun connectCalls(a: Call, b: Call) {
        (a as PjsipCall).xferReplaces(b as PjsipCall, CallOpParam(true))
        a.hangup()
    }

    fun onTic() {
        // TODO - Implement
    }

    /**
     * The pjsip account object, used to provide various callbacks about actions
     * taken by the library.
     *
     */
    private inner class Account : org.pjsip.pjsua2.Account() {

        override fun onRegState(prm: OnRegStateParam) {
            super.onRegState(prm)

            when(info.regIsActive) {
                true -> listener.onVoipProviderRegistered()
            }
        }

        override fun onIncomingCall(prm: OnIncomingCallParam) {
            super.onIncomingCall(prm)
            val call = IncomingCall(this@Account, prm.callId, listener, Invite(prm.rdata.wholeMsg).findThirdParty())
            call.acknowledge()
            listener.onIncomingCallFromVoipDriver(call)
        }
    }
}
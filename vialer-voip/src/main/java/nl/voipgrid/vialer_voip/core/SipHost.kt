package nl.voipgrid.vialer_voip.core

import java.util.*

data class SipHost(private val host: String) {

    private val prefix = "sip:"

    override fun toString(): String {
        return host
    }

    private fun transport(transport: Configuration.Transport) = ";transport=${transport.toString().toLowerCase(Locale.ENGLISH)}"

    fun withTransportAndAccount(account: String, transport: Configuration.Transport) = "${withAccount(account)}${transport(transport)}"

    fun withAccount(account: String) = "$prefix$account@$this"

    fun withTransport(transport: Configuration.Transport) = "$prefix$this${transport(transport)}"
}
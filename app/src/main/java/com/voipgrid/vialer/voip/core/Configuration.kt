package com.voipgrid.vialer.voip.core

data class Configuration(
        val host: SipHost,
        val accountId: String,
        val password: String,
        val scheme: String,
        val realm: String,
        val transport: Transport,
        val stun: Boolean,
        val userAgent: String,
        val echoCancellation: Long,
        val echoCancellationTailLength: Long,
        val codec: Codec
) {
    enum class Transport {
        UDP, TCP, TLS
    }

    enum class Codec {
        OPUS, iLBC
    }
}
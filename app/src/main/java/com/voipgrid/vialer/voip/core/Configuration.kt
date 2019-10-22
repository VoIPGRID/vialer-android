package com.voipgrid.vialer.voip.core

data class Configuration(
        val host: SipHost,
        val transport: Transport,
        val stun: Boolean,
        val userAgent: String,
        val codec: Codec,
        val scheme: String = "digest",
        val realm: String = "*",
        val echoCancellation: Long = 3,
        val echoCancellationTailLength: Long = 75
) {
    enum class Transport {
        UDP, TCP, TLS
    }

    enum class Codec {
        OPUS, iLBC
    }
}
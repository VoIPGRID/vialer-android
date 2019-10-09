package com.voipgrid.vialer.voip.providers.pjsip.initialization.config

import com.voipgrid.vialer.voip.core.Configuration
import com.voipgrid.vialer.voip.providers.pjsip.core.PjsipEndpoint

class CodecConfigurator {

    private val codecMappings = mapOf(
            "opus/48000" to Configuration.Codec.OPUS,
            "ilbc/8000" to Configuration.Codec.iLBC
    )

    fun configure(endpoint: PjsipEndpoint, configuration: Configuration) {
        val codecList = endpoint.codecEnum()

        for (i in 0 until codecList.size().toInt()) {
            val codecId = codecList.get(i).codecId
            val codec = codecMappings[codecId]

            val isSelectedCodec = configuration.codec == codec

            endpoint.codecSetPriority(codecId, when(isSelectedCodec) {
                true -> ENABLED
                false -> DISABLED
            })
        }
    }

    companion object {
        const val ENABLED: Short = 255
        const val DISABLED: Short = 0
    }
}
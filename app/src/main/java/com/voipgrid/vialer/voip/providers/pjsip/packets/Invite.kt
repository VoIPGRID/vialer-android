package com.voipgrid.vialer.voip.providers.pjsip.packets

import com.voipgrid.vialer.voip.providers.pjsip.core.ThirdParty

class Invite(private val data: String) {

    val properties = mutableMapOf<String, String>()

    private val thirdPartyPropertyPriority = listOf(
            "P-Asserted-Identity",
            "Remote-Party-ID",
            "From"
    )

    init {
        parseKeys()
    }

    /**
     * Parse the invite into a map of keys and values.
     *
     */
    private fun parseKeys() {
        data.lines().filter { it.contains(':') }.forEach {
            val (key, value) = it.split(":", limit = 2)
            properties[key] = value
        }
    }

    /**
     * Extract a property that looks like a third party header.
     *
     */
    private fun extractThirdPartyProperty(key: String): ThirdParty? {
        val value = properties[key] ?: return null

        val (_,name, number) = Regex("^ ?\"(.+)\" <?sip:(.+)@").find(value)?.groupValues ?: return null

        return ThirdParty(number, name)
    }

    /**
     * Find the relevant third party header.
     *
     */
    fun findThirdParty(): ThirdParty {
        thirdPartyPropertyPriority.forEach { key ->
            extractThirdPartyProperty(key)?.let {
                return it
            }
        }

        return extractThirdPartyProperty("From") ?: throw Exception("There is no third party information in this INVITE, something has gone wrong.")
    }
}
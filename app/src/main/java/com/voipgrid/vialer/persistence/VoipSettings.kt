package com.voipgrid.vialer.persistence

import com.chibatching.kotpref.enumpref.enumValuePref
import com.voipgrid.vialer.User
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object VoipSettings : DefaultKotPrefModel() {

    enum class AudioCodec {
        iLBC, OPUS
    }

    var isAccountSetupForSip by booleanPref(key = "PREF_HAS_SIP_PERMISSION", default = false)
    var hasEnabledSip by booleanPref(key = "PREF_HAS_SIP_ENABLED", default = true)
    val canUseSip get() = isAccountSetupForSip && hasEnabledSip && User.hasVoipAccount

    var wantsToUse3GForCalls by booleanPref(key = "PREF_HAS_3G_ENABLED", default = true)

    var hasTlsEnabled by booleanPref(key = "PREF_HAS_TLS_ENABLED", default = true)
    var hasStunEnabled by booleanPref(key = "PREF_HAS_STUN_ENABLED", default = false)
    var audioCodec by enumValuePref(default = AudioCodec.OPUS)
}


package com.voipgrid.vialer.persistence

import com.chibatching.kotpref.enumpref.enumValuePref
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object Middleware : DefaultKotPrefModel() {
    enum class RegistrationStatus {
        FAILED, REGISTERED, UNREGISTERED, UPDATE_NEEDED
    }

    var currentToken by stringPref()
}


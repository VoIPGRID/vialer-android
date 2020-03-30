package com.voipgrid.vialer.persistence

import com.chibatching.kotpref.enumpref.enumValuePref
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object Middleware : DefaultKotPrefModel() {
    var currentToken by stringPref()
}


package com.voipgrid.vialer.persistence

import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object Statistics : DefaultKotPrefModel() {
    var numberOfCalls by intPref()
}


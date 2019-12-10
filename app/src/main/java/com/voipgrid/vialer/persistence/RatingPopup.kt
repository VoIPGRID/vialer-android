package com.voipgrid.vialer.persistence

import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object RatingPopup : DefaultKotPrefModel() {
    var shown by booleanPref(default = false)
}


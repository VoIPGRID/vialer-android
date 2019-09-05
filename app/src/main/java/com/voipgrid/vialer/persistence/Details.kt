package com.voipgrid.vialer.persistence

import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.voipgrid.vialer.api.PhoneAccountFetcher
import com.voipgrid.vialer.api.models.InternalNumbers
import com.voipgrid.vialer.api.models.PhoneAccounts
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object Internal : DefaultKotPrefModel() {

    var phoneAccounts by gsonNullablePref(PhoneAccounts(), PhoneAccounts::class.java.name)
    var phoneAccountsCache by gsonNullablePref(PhoneAccountFetcher.PhoneAccountsCache(), PhoneAccountFetcher.PhoneAccountsCache::class.java.name)
    var internalNumbers by gsonNullablePref(InternalNumbers(), InternalNumbers::class.java.name)
    var lastDialledNumber by stringPref(default = "")
}
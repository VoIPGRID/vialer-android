package com.voipgrid.vialer.persistence

import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.chibatching.kotpref.gsonpref.gsonPref
import com.voipgrid.vialer.api.PhoneAccountFetcher
import com.voipgrid.vialer.api.models.UserDestination
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel
import org.joda.time.DateTime

object Internal : DefaultKotPrefModel() {

    var phoneAccountsCache by gsonNullablePref(PhoneAccountFetcher.PhoneAccountsCache(), PhoneAccountFetcher.PhoneAccountsCache::class.java.name)
    var lastDialledNumber by stringPref(default = "")
    var callRecordMonthsImported by gsonPref(mutableListOf<DateTime>())

    var destinations by gsonPref(listOf<UserDestination>(), "destinations")

    val phoneNumbers: List<String>
        get() = destinations.map { it.internalNumber }.toMutableList().plus(destinations.flatMap { it.phoneAccounts }.map { it.number })

    val phoneAccounts: List<String>
        get() = destinations.flatMap { it.phoneAccounts }.map { it.id }
}
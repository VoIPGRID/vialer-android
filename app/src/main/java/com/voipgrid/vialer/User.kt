package com.voipgrid.vialer

import com.chibatching.kotpref.gsonpref.gsonNullablePref
import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.persistence.*
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object User : DefaultKotPrefModel() {

    @JvmField val voip = VoipSettings
    @JvmField val internal = Internal
    @JvmField val middleware = Middleware
    @JvmField val userPreferences = UserPreferences

    @JvmStatic var voipgridUser by gsonNullablePref<SystemUser>(null, SystemUser::class.java.name)
    @JvmStatic var voipAccount by gsonNullablePref<PhoneAccount>(null, PhoneAccount::class.java.name)

    @JvmStatic val isLoggedIn get() = voipgridUser != null
    @JvmStatic val hasVoipAccount get() = voipAccount != null

    @JvmStatic var loginToken by stringPref()
    @JvmStatic var username by stringPref()
    @JvmStatic var uuid by stringPref()
}
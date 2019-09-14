package com.voipgrid.vialer.persistence

import android.os.Build
import com.chibatching.kotpref.enumpref.enumValuePref
import com.voipgrid.vialer.BuildConfig
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object UserPreferences : DefaultKotPrefModel() {

    enum class ConnectionPreference {
        ONLY_CELLULAR, CEULLAR_AND_WIFI, SHOW_POPUP_BEFORE_EVERY_CALL
    }

    /**
     * We are storing the connection preference in an internal property so we can override
     * the return value easily for different android sdks.
     *
     */
    private var internalConnectionPreference by enumValuePref(ConnectionPreference.CEULLAR_AND_WIFI)

    /**
     * The connection preference, whether or not to prompt to change networks when calling.
     *
     */
    var connectionPreference : ConnectionPreference
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return ConnectionPreference.CEULLAR_AND_WIFI
            }

            return internalConnectionPreference
        }
        set(value) {
            internalConnectionPreference = value
        }

    /**
     * Allows easy querying of the preference.
     *
     */
    fun hasConnectionPreference(preference: ConnectionPreference): Boolean {
        return preference == connectionPreference
    }

    /**
     * If enabled, will take the ringtone from the device rather than the Vialer ringtone.
     *
     */
    var usePhoneRingtone by booleanPref(key = "use_phone_ringtone", default = false)
}


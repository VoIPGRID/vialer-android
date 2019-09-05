package com.voipgrid.vialer.persistence

import android.os.Build
import com.chibatching.kotpref.enumpref.enumValuePref
import com.voipgrid.vialer.BuildConfig
import com.voipgrid.vialer.persistence.core.DefaultKotPrefModel

object UserPreferences : DefaultKotPrefModel() {

    enum class ConnectionPreference {
        NONE, WIFI, LTE
    }

    /**
     * We are storing the connection preference in an internal property so we can override
     * the return value easily for different android sdks.
     *
     */
    private var internalConnectionPreference by enumValuePref(ConnectionPreference.NONE)

    /**
     * The connection preference, whether or not to prompt to change networks when calling.
     *
     */
    var connectionPreference : ConnectionPreference
        get() {
            if (Build.VERSION.SDK_INT >= BuildConfig.ANDROID_Q_SDK_VERSION) {
                return ConnectionPreference.WIFI
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
     * Whether the user has decided to only show missed calls on their call records screen.
     *
     */
    var displayMissedCallsOnly by booleanPref(key = "PREF_DISPLAY_MISSED_CALLS_ONLY", default = false)
}


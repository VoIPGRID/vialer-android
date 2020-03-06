package com.voipgrid.vialer.settings

import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.voipgrid.vialer.ActivityLifecycleTracker
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.SecureCalling
import com.voipgrid.vialer.persistence.UserPreferences
import com.voipgrid.vialer.persistence.VoipSettings
import org.koin.android.ext.android.inject


class AdvancedCallSettingsFragment : AbstractSettingsFragment() {

    private val secureCalling: SecureCalling by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_advanced_call, rootKey)

        findPreference<ListPreference>("audioCodec")?.apply {
            summaryProvider = Preference.SummaryProvider<ListPreference> {
                preference -> preference.entries[preference.findIndexOfValue(VoipSettings.audioCodec.toString())]
            }
            entries = arrayOf(getString(R.string.call_codec_standard_quality), getString(R.string.call_codec_high_quality))
            entryValues = arrayOf(VoipSettings.AudioCodec.iLBC.toString(), VoipSettings.AudioCodec.OPUS.toString())
            setDefaultValue(VoipSettings.AudioCodec.OPUS.toString())
        }

        findPreference<PreferenceCategory>("settings_advanced_call_category_network")?.isVisible =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        findPreference<ListPreference>("internalConnectionPreference")?.apply {
            isVisible = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            summaryProvider = Preference.SummaryProvider<ListPreference> {
                preference ->
                val index = preference.findIndexOfValue(User.userPreferences.connectionPreference.toString())
                preference.entries[index]
            }
            entries = arrayOf(
                    getString(R.string.call_connection_only_cellular),
                    getString(R.string.call_connection_use_wifi_cellular),
                    getString(R.string.call_connection_optional)
            )
            entryValues = arrayOf(
                    UserPreferences.ConnectionPreference.ONLY_CELLULAR.toString(),
                    UserPreferences.ConnectionPreference.CEULLAR_AND_WIFI.toString(),
                    UserPreferences.ConnectionPreference.SHOW_POPUP_BEFORE_EVERY_CALL.toString()
            )
            setDefaultValue(UserPreferences.ConnectionPreference.CEULLAR_AND_WIFI.toString())
        }

        findPreference<SwitchPreferenceCompat>("PREF_HAS_TLS_ENABLED")
                ?.setOnChangeListener(networkConnectivityRequired = true) { enableTls: Boolean ->
            isLoading = true

            if (enableTls) {
                secureCalling.enable(callback)
            } else {
                secureCalling.disable(callback)
            }

            true
        }
    }

    private val callback = object : SecureCalling.Callback {
        override fun onSuccess() {
            isLoading = false
            ActivityLifecycleTracker.removeEncryptionNotification()
        }

        override fun onFail() {
            isLoading = false
        }
    }
}
package com.voipgrid.vialer.settings

import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.persistence.UserPreferences
import com.voipgrid.vialer.persistence.VoipSettings

class AdvancedCallSettingsFragment  : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_call_settings, rootKey)

        findPreference<ListPreference>("audioCodec")?.apply {
            summaryProvider = Preference.SummaryProvider<ListPreference> { preference -> preference.entries[preference.findIndexOfValue(VoipSettings.audioCodec.toString())] }
            entries = arrayOf(getString(R.string.call_codec_standard_quality), getString(R.string.call_codec_high_quality))
            entryValues = arrayOf(VoipSettings.AudioCodec.iLBC.toString(), VoipSettings.AudioCodec.OPUS.toString())
            setDefaultValue(VoipSettings.AudioCodec.OPUS.toString())
        }

        findPreference<ListPreference>("internalConnectionPreference")?.apply {
            isVisible = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            summaryProvider = Preference.SummaryProvider<ListPreference> { preference -> preference.entries[preference.findIndexOfValue(User.userPreferences.connectionPreference.toString())] }
            entries = arrayOf(getString(R.string.call_connection_only_cellular), getString(R.string.call_connection_use_wifi_cellular), getString(R.string.call_connection_optional))
            entryValues = arrayOf(UserPreferences.ConnectionPreference.ONLY_CELLULAR.toString(), UserPreferences.ConnectionPreference.CEULLAR_AND_WIFI.toString(), UserPreferences.ConnectionPreference.SHOW_POPUP_BEFORE_EVERY_CALL.toString())
            setDefaultValue(UserPreferences.ConnectionPreference.CEULLAR_AND_WIFI.toString())
        }
    }
}
package com.voipgrid.vialer.settings

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.preference.*
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.persistence.UserPreferences
import com.voipgrid.vialer.persistence.VoipSettings
import com.voipgrid.vialer.util.BatteryOptimizationManager

class SettingsFragment : PreferenceFragmentCompat() {

    private val batteryOptimizationManager by lazy {
        BatteryOptimizationManager(activity ?: throw Exception("No activity"))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<EditTextPreference>("voip_account")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipAccount?.accountId }
        findPreference<EditTextPreference>("mobile_number")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.mobileNumber }
        findPreference<EditTextPreference>("outgoing_number")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.outgoingCli }

        findPreference<SwitchPreferenceCompat>("PREF_REMOTE_LOGGING")?.summaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> { if (User.remoteLogging.isEnabled) User.remoteLogging.id else "" }

        findPreference<SwitchPreferenceCompat>("battery_optimisation")?.apply {
            isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
            setOnPreferenceChangeListener { _: Preference, _: Any ->
                batteryOptimizationManager.prompt(activity ?: throw Exception("No activity"), false)
                true
            }
        }
    }
}
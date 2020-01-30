package com.voipgrid.vialer.settings

import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.BatteryOptimizationManager
import org.koin.android.ext.android.inject

class RunInBackgroundSettingsFragment : AbstractSettingsFragment() {

    private val batteryOptimizationManager: BatteryOptimizationManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_run_in_background, rootKey)

        findPreference<SwitchPreferenceCompat>("battery_optimization")?.apply {
            isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
            setOnChangeListener<Boolean> {
                batteryOptimizationManager.prompt(activity ?: throw Exception("No activity"), false)
                true
            }
        }

        (activity as SettingsActivity).onActivityResultCallback = {
            findPreference<SwitchPreferenceCompat>("battery_optimization")?.isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<SwitchPreferenceCompat>("battery_optimization")?.isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
    }
}
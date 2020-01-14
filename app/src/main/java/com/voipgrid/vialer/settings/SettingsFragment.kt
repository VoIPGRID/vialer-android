package com.voipgrid.vialer.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.notifications.VoipDisabledNotification
import com.voipgrid.vialer.onboarding.SingleOnboardingStepActivity.Companion.launch
import com.voipgrid.vialer.onboarding.steps.MissingVoipAccountStep
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.BatteryOptimizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SettingsFragment : AbstractSettingsFragment() {

    private val batteryOptimizationManager: BatteryOptimizationManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<Preference>("feedback")?.setOnPreferenceClickListener {
            activity?.let {
                FeedbackDialogFragment().show(it.supportFragmentManager, "")
            }

            false
        }

        findPreference<SwitchPreferenceCompat>("PREF_HAS_SIP_ENABLED")?.setOnPreferenceChangeListener { _: Preference, voipEnabled: Any ->
            callUsingVoipChanged(voipEnabled as Boolean)
            true
        }

        findPreference<Preference>("PREF_REMOTE_LOGGING_ID")?.summaryProvider = Preference.SummaryProvider<Preference> { if (User.remoteLogging.isEnabled) User.remoteLogging.id else "" }

        findPreference<SwitchPreferenceCompat>("PREF_REMOTE_LOGGING")?.apply {
            setOnPreferenceChangeListener { _: Preference, _: Any ->
                GlobalScope.launch(Dispatchers.Main) {
                    delay(1000)
                    MiddlewareHelper.registerAtMiddleware(activity)
                }

                true
            }
        }

        findPreference<SwitchPreferenceCompat>("battery_optimisation")?.apply {
            isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
            setOnPreferenceChangeListener {  _: Preference, _: Any ->
                batteryOptimizationManager.prompt(activity ?: throw Exception("No activity"), false)
                true
            }
        }

        (activity as SettingsActivity).onActivityResultCallback = {
            findPreference<SwitchPreferenceCompat>("battery_optimisation")?.isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<SwitchPreferenceCompat>("battery_optimisation")?.isChecked = batteryOptimizationManager.isIgnoringBatteryOptimization()
    }

    /**
     * If the user has changed the voip setting we must correct update the middleware
     *
     */
    private fun callUsingVoipChanged(voipEnabled: Boolean) {
        if (!voipEnabled) {
            MiddlewareHelper.unregister(activity)
            activity?.stopService(Intent(activity, SipService::class.java))
            return
        }

        isLoading = true

        userSynchronizer.syncWithCallback(Dispatchers.Main) {
            isLoading = false

            if (!User.hasVoipAccount) {
                activity?.runOnUiThread {
                    launch(activity ?: throw Exception(""), MissingVoipAccountStep::class.java)
                }

                return@syncWithCallback
            }

            VoipDisabledNotification().remove()
        }
    }
}
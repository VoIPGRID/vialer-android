package com.voipgrid.vialer.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.UserSynchronizer
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
import javax.inject.Inject


class SettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userSynchronizer: UserSynchronizer

    private val batteryOptimizationManager by lazy {
        BatteryOptimizationManager(activity ?: throw Exception("No activity"))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        VialerApplication.get().component().inject(this)

        findPreference<Preference>("feedback")?.setOnPreferenceClickListener {
            activity?.let {

                FeedbackDialogFragment().show(it.supportFragmentManager, "")
            }

            false
        }

        findPreference<SwitchPreferenceCompat>("PREF_HAS_SIP_ENABLED")?.setOnPreferenceChangeListener { _: Preference, voipEnabled: Any ->
            if (voipEnabled == false) {
                MiddlewareHelper.unregister(activity)
                activity?.stopService(Intent(activity, SipService::class.java))
                return@setOnPreferenceChangeListener true
            }

            isLoading = true

            GlobalScope.launch(Dispatchers.Main) {

                userSynchronizer.sync()
                isLoading = false

                if (!User.hasVoipAccount) {
                    activity?.runOnUiThread {
                        launch(activity ?: throw Exception(""), MissingVoipAccountStep::class.java)
                    }
                    return@launch
                }

                VoipDisabledNotification().remove()
            }


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
            callChangeListener {
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
}
package com.voipgrid.vialer.settings

import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.InputType
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.models.MobileNumber
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


class AccountSettingsFragment : AbstractSettingsFragment() {

    private val telephonyManager: TelephonyManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_account, rootKey)

        findPreference<EditTextPreference>("name")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.fullName }
        findPreference<EditTextPreference>("username")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.email }
        findPreference<EditTextPreference>("description")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipAccount?.description }
        findPreference<EditTextPreference>("account_id")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipAccount?.accountId }
        findPreference<EditTextPreference>("outgoing_number")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.outgoingCli }
        findPreference<Preference>("mobile_number_not_matching")?.apply {
            isVisible = canFindMobileNumber() && !configuredMobileNumberMatchesSimPhoneNumber()
            summaryProvider = Preference.SummaryProvider<Preference> {
                try {
                    telephonyManager.line1Number
                } catch (e: SecurityException) {
                    ""
                }
            }
            setOnPreferenceClickListener {
                try {
                    mobileNumberChanged(telephonyManager.line1Number)
                } catch (e: SecurityException) { }

                true
            }
        }

        findPreference<EditTextPreference>("mobile_number")?.apply {
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_PHONE
                editText.text.clear()
                editText.text.insert(0, User.voipgridUser?.mobileNumber)
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.mobileNumber }
            setOnPreferenceChangeListener { _: Preference, newValue: Any -> mobileNumberChanged(newValue as String) }
        }
    }

    /**
     * Validate the mobile number, send the change request to the server and then refresh our user to
     * confirm that the change has been applied.
     *
     */
    private fun mobileNumberChanged(newNumber: String) : Boolean {
        if (!PhoneNumberUtils.isValidMobileNumber(newNumber)) {
            alert(R.string.phonenumber_info_text_title, R.string.onboarding_account_configure_invalid_phone_number)
            return false
        }

        val number: String = PhoneNumberUtils.formatMobileNumber(newNumber)

        isLoading = true

        GlobalScope.launch(Dispatchers.IO) {
            val response = voipgridApi.mobileNumber(MobileNumber(number)).execute()

            if (!response.isSuccessful) {
                alert(R.string.phonenumber_info_text_title, R.string.onboarding_account_configure_invalid_phone_number)
                return@launch
            }

            userSynchronizer.sync()

            activity?.runOnUiThread {
                refreshSummary<EditTextPreference>("mobile_number")
                findPreference<Preference>("mobile_number_not_matching")?.isVisible = canFindMobileNumber() && !configuredMobileNumberMatchesSimPhoneNumber()
                isLoading = false
            }
        }

        return true
    }

    private fun configuredMobileNumberMatchesSimPhoneNumber() : Boolean {
        return try {
            telephonyManager.line1Number == User.voipgridUser?.mobileNumber
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * See if we can find the mobile number, not all manufacturers allow this.
     *
     */
    private fun canFindMobileNumber() : Boolean  = try {
        telephonyManager.line1Number != null && telephonyManager.line1Number.isNotEmpty()
    } catch (e: SecurityException) {
        false
    }

}
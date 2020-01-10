package com.voipgrid.vialer.settings

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.models.MobileNumber
import com.voipgrid.vialer.util.PhoneNumberUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class VoipSettingsFragment : PreferenceFragmentCompat(), Callback<MobileNumber> {

    private val voipgridApi by lazy { ServiceGenerator.createApiService(activity ?: throw Exception("No activity")) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.voip_settings, rootKey)

        findPreference<EditTextPreference>("voip_account")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipAccount?.accountId }
        findPreference<EditTextPreference>("outgoing_number")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.outgoingCli }


        findPreference<EditTextPreference>("mobile_number")?.apply {
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_PHONE
                editText.text.clear()
                editText.text.insert(0, User.voipgridUser?.mobileNumber)
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.mobileNumber }
            setOnPreferenceChangeListener { _: Preference, newValue: Any ->
                if (!PhoneNumberUtils.isValidMobileNumber(newValue as String)) {
                    return@setOnPreferenceChangeListener false
                }

                val number: String = PhoneNumberUtils.formatMobileNumber(newValue)

                voipgridApi
                        .mobileNumber(MobileNumber(number))
                        .enqueue(this@VoipSettingsFragment)

                true
            }
        }
    }

    override fun onFailure(call: Call<MobileNumber>, t: Throwable) {
    }

    override fun onResponse(call: Call<MobileNumber>, response: Response<MobileNumber>) {

    }
}
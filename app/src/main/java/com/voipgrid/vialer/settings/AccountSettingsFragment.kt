package com.voipgrid.vialer.settings

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.api.models.MobileNumber
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


class AccountSettingsFragment : AbstractSettingsFragment(), Callback<MobileNumber> {

    @Inject lateinit var userSynchronizer: UserSynchronizer

    private val voipgridApi by lazy { ServiceGenerator.createApiService(activity ?: throw Exception("No activity")) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.voip_settings, rootKey)
        VialerApplication.get().component().inject(this)

        findPreference<EditTextPreference>("name")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.fullName }
        findPreference<EditTextPreference>("username")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipgridUser?.email }
        findPreference<EditTextPreference>("description")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { User.voipAccount?.description }
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
                    alert(R.string.phonenumber_info_text_title, R.string.onboarding_account_configure_invalid_phone_number)
                    return@setOnPreferenceChangeListener false
                }

                val number: String = PhoneNumberUtils.formatMobileNumber(newValue)

                voipgridApi
                        .mobileNumber(MobileNumber(number))
                        .enqueue(this@AccountSettingsFragment)

                isLoading = true
                true
            }
        }
    }

    override fun onFailure(call: Call<MobileNumber>, t: Throwable) {
        isLoading = false
        alert(R.string.phonenumber_info_text_title, R.string.onboarding_account_configure_invalid_phone_number)
    }

    override fun onResponse(call: Call<MobileNumber>, response: Response<MobileNumber>) {
        if (!response.isSuccessful) {
            isLoading = false
            alert(R.string.phonenumber_info_text_title, R.string.onboarding_account_configure_invalid_phone_number)
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            userSynchronizer.sync()
            findPreference<EditTextPreference>("mobile_number")?.summaryProvider = findPreference<EditTextPreference>("mobile_number")?.summaryProvider
            isLoading = false
        }
    }
}
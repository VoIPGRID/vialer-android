package com.voipgrid.vialer.onboarding.steps

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import com.voipgrid.vialer.*
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.api.models.MobileNumber
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.android.synthetic.main.onboarding_step_mobile_number.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AccountConfigurationStep : Step(), View.OnClickListener {

    override val layout = R.layout.onboarding_step_mobile_number

    private val outgoingNumber: String?
        get() = if (User.voipgridUser?.outgoingCli == "suppressed") onboarding?.getString(R.string.supressed_number) else User.voipgridUser?.outgoingCli

    private val logger = Logger(this).forceRemoteLogging(true)

    @Inject lateinit var userSynchronizer: UserSynchronizer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)
    }

    override fun onResume() {
        super.onResume()

        if (!User.isLoggedIn) {
            logger.e("The user has made it to mobile number without a successful login, restart onboarding")
            onboarding?.restart()
            return
        }

        mobile_number_text_dialog.setText(User.voipgridUser?.mobileNumber)
        mobile_number_text_dialog.setRightDrawableOnClickListener {
            alert(R.string.phonenumber_info_text_title, R.string.phonenumber_info_text)
        }
        mobile_number_text_dialog.onTextChanged {
            button_configure.isEnabled = mobile_number_text_dialog.text?.isNotEmpty() ?: false
        }
        outgoing_number_tv.text = outgoingNumber
        button_configure.setOnClickListenerAndDisable(this)
        button_configure.isEnabled = mobile_number_text_dialog.text?.isNotEmpty() ?: false
    }

    override fun onClick(view: View?) {
        val mobileNumber = mobile_number_text_dialog.text.toString()

        if (!PhoneNumberUtils.isValidMobileNumber(mobileNumber)) {
            error(R.string.invalid_mobile_number_message, R.string.invalid_mobile_number_message)
            return
        }

        mobile_number_text_dialog.clearFocus()

        val formattedMobileNumber = PhoneNumberUtils.format(mobileNumber)

        when (hasOutgoingNumber()) {
            true  -> configureUserAccount(formattedMobileNumber)
            false -> informUserTheyHaveNoOutgoingNumber(formattedMobileNumber)
        }
    }

    /**
     * Perform various configuration and checks on the user account to make it is set-up correctly
     * to work with the app.
     *
     */
    private fun configureUserAccount(mobileNumber: String) = GlobalScope.launch(Dispatchers.Main) {
        if (!PhoneNumberUtils.isValidMobileNumber(mobileNumber)) {
            error()
            return@launch
        }

        try {
            updateUserMobileNumber(mobileNumber)

            userSynchronizer.sync()

            if (!User.hasVoipAccount) {
                logger.w("There is no linked voip account, prompt user to configure one")
                state?.hasVoipAccount = false
                onboarding?.progress(this@AccountConfigurationStep)
                return@launch
            }

            onboarding?.progress(this@AccountConfigurationStep)
        } catch (e: Exception) {
            logger.e("Failed to configure account: ${e.message}")
            error()
        }
    }

    /**
     * Update the mobile number stored by the VoIPGRID api and update our local versions too.
     *
     */
    private suspend fun updateUserMobileNumber(mobileNumber: String) = withContext(Dispatchers.IO)  {
        val response = voipgridApi.mobileNumber(MobileNumber(mobileNumber)).execute()

        if (!response.isSuccessful) {
            throw Exception("It seems the mobile number is not valid: $mobileNumber")
        }

        logger.i("Received a successful mobile number response, looking for a linked phone account")

        User.voipgridUser?.let {
            it.mobileNumber = mobileNumber
            it.outgoingCli = outgoingNumber
            User.voipgridUser = it
        }
    }

    private fun error() = activity?.runOnUiThread {
        error(R.string.onboarding_account_configure_failed_title, R.string.onboarding_account_configure_invalid_phone_number)
        button_configure.isEnabled = true
    }

    /**
     * Display a dialog box to let the user know they have no outgoing number set.
     *
     */
    private fun informUserTheyHaveNoOutgoingNumber(mobileNumber: String) = activity?.runOnUiThread {
        button_configure.isEnabled = true
        AlertDialog.Builder(onboarding)
                .setTitle(R.string.onboarding_account_configure_no_outgoing_number_title)
                .setMessage(R.string.onboarding_account_configure_no_outgoing_number)
                .setPositiveButton(android.R.string.yes) { _, _ -> configureUserAccount(mobileNumber) }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun hasOutgoingNumber() = outgoing_number_tv.text.isNotEmpty()
}
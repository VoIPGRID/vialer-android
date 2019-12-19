package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onTextChanged
import com.voipgrid.vialer.onboarding.OnboardingActivity
import com.voipgrid.vialer.onboarding.VoipgridLogin
import com.voipgrid.vialer.onboarding.core.OnboardingState
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.TwoFactorFragmentHelper
import com.voipgrid.vialer.voipgrid.PasswordResetWebActivity
import kotlinx.android.synthetic.main.onboarding_step_two_factor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class TwoFactorStep : Step() {

    override val layout = R.layout.onboarding_step_two_factor

    @Inject lateinit var login: VoipgridLogin

    private val logger = Logger(this).forceRemoteLogging(true)
    private lateinit var password: String
    private lateinit var username: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)

        val usernameCheck = (onboarding as OnboardingActivity).getUsername()
        val passwordCheck = (onboarding as OnboardingActivity).getPassword()
        if (usernameCheck == null || passwordCheck == null) {
            logger.e("The user has made it to two factor authentication without passing values for e-mail or password, restart onboarding")
            onboarding?.restart()
            return
        }

        username = usernameCheck
        password = passwordCheck
        (onboarding as OnboardingActivity).setCredentialsForTfa("", "")

        TwoFactorFragmentHelper(context, pin_view).apply {
            if (pasteCodeFromClipboard()) {
                pin_view.clearFocus()
                attemptLogin(pin_view.text.toString())
            }
        }

        pin_view.setOnFocusChangeListener { _, _ -> showDefaultInput() }
        pin_view.onTextChanged {
            if (pin_view.text == null || pin_view.text?.length != 6) {
                return@onTextChanged
            }
            pin_view.clearFocus()
            attemptLogin(pin_view.text.toString())
        }

        button_back.setOnClickListener { activity?.onBackPressed() }
    }

    /**
     * Perform different actions based on the result of an attempted login.
     *
     */
    private fun handleLoginResult(result: VoipgridLogin.LoginResult) = when(result) {
        VoipgridLogin.LoginResult.FAIL -> {
            logger.w("User failed to login to VoIPGRID")
            login.lastError?.let { showErrorInput() }
        }
        VoipgridLogin.LoginResult.SUCCESS -> {
            logger.i("Login to VoIPGRID was successful, progressing the user in onboarding")
            onboarding?.progress(this)
        }
        VoipgridLogin.LoginResult.TWO_FACTOR_REQUIRED -> {
            logger.i("User logged into VoIPGRID with the correct username/password but is now required to input a valid 2FA code")
        }
        VoipgridLogin.LoginResult.MUST_CHANGE_PASSWORD -> {
            logger.i("User must change their password before we can login")
            activity?.let {
                PasswordResetWebActivity.launch(it, username, password)
            }
        }
    }

    /**
     * Attempt to log the user into VoIPGRID by launching a co-routine.
     *
     */
    private fun attemptLogin(code: String? = null) = GlobalScope.launch(Dispatchers.Main) {
        if (username.isEmpty() || password.isEmpty()) {
            return@launch
        }
        onboarding?.isLoading = true
        logger.i("Attempting to log the user into VoIPGRID, with the following 2FA code: $code")
        val result = login.attempt(username, password, code)
        onboarding?.isLoading = false
        handleLoginResult(result)
    }

    /**
     * Sets the colors of the text of the input fields to red and shows error labels and button.
     */
    private fun showErrorInput() {
        pin_view.setTextColor(ContextCompat.getColor((onboarding as Context), R.color.error_color))
        error_label.visibility = View.VISIBLE
        error_description_label.visibility = View.VISIBLE
        button_enter_backup_code.visibility = View.VISIBLE
    }

    /**
     * Sets the colors of the text of the input fields to default and hides error labels and button.
     */
    private fun showDefaultInput() {
        pin_view.setTextColor(ContextCompat.getColor((onboarding as Context), R.color.onboarding_button_text_color))
        error_label.visibility = View.GONE
        error_description_label.visibility = View.GONE
        button_enter_backup_code.visibility = View.GONE
    }

    override fun shouldThisStepBeSkipped(state: OnboardingState): Boolean {
        return state.skipTwoFactor
    }
}

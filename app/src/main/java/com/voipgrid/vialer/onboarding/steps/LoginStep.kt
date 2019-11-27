package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.annotation.LayoutRes
import com.voipgrid.vialer.*
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.OnboardingActivity
import com.voipgrid.vialer.onboarding.VoipgridLogin
import com.voipgrid.vialer.onboarding.VoipgridLogin.LoginResult
import com.voipgrid.vialer.onboarding.VoipgridLogin.LoginResult.*
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.voipgrid.PasswordResetWebActivity
import kotlinx.android.synthetic.main.onboarding_step_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import javax.inject.Inject

class LoginStep : Step() {

    override val layout = R.layout.onboarding_step_login

    @Inject lateinit var connectivityHelper: ConnectivityHelper
    @Inject lateinit var login: VoipgridLogin

    private val logger = Logger(this).forceRemoteLogging(true)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)

        val enableSubmitButton: (_: Editable?) -> Unit = {
            button_login.isEnabled = username_text_dialog.length() > 0 && password_text_dialog.length() > 0
        }

        username_text_dialog.onTextChanged(enableSubmitButton)
        password_text_dialog.onTextChanged(enableSubmitButton)
        username_text_dialog.setOnFocusChangeListener { _, _ -> showDefaultInputs() }
        password_text_dialog.setOnFocusChangeListener { _, _ -> showDefaultInputs() }
        showDefaultInputs()

        password_text_dialog.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent? ->
        KeyboardVisibilityEvent.setEventListener(activity) { keyboardIsVisible ->
            if (keyboardIsVisible) {
                title_label.visibility = View.GONE
                subtitle_label.visibility = View.GONE
            } else {
                title_label.visibility = View.VISIBLE
                subtitle_label.visibility = View.VISIBLE
            }
        }

        password_text_dialog.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent? ->
            actionId == EditorInfo.IME_ACTION_DONE && button_login.performClick()
        }

        button_login.setOnClickListenerAndDisable {
            username_text_dialog.clearFocus()
            password_text_dialog.clearFocus()
            attemptLogin()
        }
        button_forgot_password.setOnClickListener { launchForgottenPasswordActivity() }

        automaticallyLogInIfWeHaveCredentials()
    }

    override fun onResume() {
        super.onResume()
        twoFactorHelper?.pasteCodeFromClipboard()
        User.internal.hasCompletedOnBoarding = false
    }

    override fun isSameAs(step: Step?) = step is LoginStep

    /**
     * If this activity has been started with an intent containing credentials, log in with them automatically.
     *
     */
    private fun automaticallyLogInIfWeHaveCredentials() {
        val intent = activity?.intent ?: return

        if (!intent.hasExtra(PasswordResetWebActivity.USERNAME_EXTRA) || !intent.hasExtra(PasswordResetWebActivity.PASSWORD_EXTRA)) {
            return
        }

        username_text_dialog.setText(intent.getStringExtra(PasswordResetWebActivity.USERNAME_EXTRA))
        password_text_dialog.setText(intent.getStringExtra(PasswordResetWebActivity.PASSWORD_EXTRA))
        if (button_login.isEnabled) button_login.performClick()
    }

    /**
     * Attempt to log the user into VoIPGRID by launching a co-routine.
     *
     */
    private fun attemptLogin(code: String? = null) = GlobalScope.launch(Dispatchers.Main) {
        onboarding?.isLoading = true
        logger.i("Attempting to log the user into VoIPGRID, with the following 2FA code: $code")
        val result = login.attempt(username_text_dialog.text.toString(), password_text_dialog.text.toString(), code)
        onboarding?.isLoading = false
        handleLoginResult(result)
    }

    /**
     * Perform different actions based on the result of an attempted login.
     *
     */
    private fun handleLoginResult(result: LoginResult) = when(result) {
        FAIL -> {
            logger.w("User failed to login to VoIPGRID")
            login.lastError?.let { showErrorInputs() }
            button_login.isEnabled = true
        }
        SUCCESS -> {
            logger.i("Login to VoIPGRID was successful, progressing the user in onboarding")
            onboarding?.progress(this)
        }
        TWO_FACTOR_REQUIRED -> {
            logger.i("User logged into VoIPGRID with the correct username/password but is now required to input a valid 2FA code")
            button_login.isEnabled = true
            state?.skipTwoFactor = false
            (onboarding as OnboardingActivity).setCredentialsForTfa(username_text_dialog.text.toString(), password_text_dialog.text.toString())
            onboarding?.progress(this)
        }
        MUST_CHANGE_PASSWORD -> {
            logger.i("User must change their password before we can login")
            activity?.let {
                PasswordResetWebActivity.launch(it, username_text_dialog.text.toString(), password_text_dialog.text.toString())
            }
        }
    }

    /**
     * Launches an activity to allow the user to reset their password.
     *
     */
    private fun launchForgottenPasswordActivity() {
        logger.i("Detected forgot password click, launching activity")
        ForgottenPasswordActivity.launchForEmail(onboarding as Context, username_text_dialog.text.toString())
    }

    /**
     * Sets the colors of the text and border of the input fields to red, displays an exclamation
     * mark in the text fields and displays an error text.
     */
    private fun showErrorInputs() {
        username_text_dialog.setTextColor(ContextCompat.getColor(onboarding as Context, R.color.error_color))
        username_text_dialog.backgroundTintList = ContextCompat.getColorStateList(onboarding as Context, R.color.error_color)
        username_text_dialog.compoundDrawablesRelative[2].alpha = 255
        password_text_dialog.setTextColor(ContextCompat.getColor(onboarding as Context, R.color.error_color))
        password_text_dialog.backgroundTintList = ContextCompat.getColorStateList(onboarding as Context, R.color.error_color)
        password_text_dialog.compoundDrawablesRelative[2].alpha = 255
        text_error.visibility = View.VISIBLE
    }

    /**
     * Sets the colors of the text and border of the input fields to default, removes the
     * exclamation mark from the text fields and hides the error text.
     */
    private fun showDefaultInputs() {
        username_text_dialog.setTextColor(ContextCompat.getColor(onboarding as Context, R.color.onboarding_text_hint_color))
        username_text_dialog.backgroundTintList = ContextCompat.getColorStateList(onboarding as Context, R.color.onboarding_text_hint_color)
        username_text_dialog.compoundDrawablesRelative[2].alpha = 0
        password_text_dialog.setTextColor(ContextCompat.getColor(onboarding as Context, R.color.onboarding_text_hint_color))
        password_text_dialog.backgroundTintList = ContextCompat.getColorStateList(onboarding as Context, R.color.onboarding_text_hint_color)
        password_text_dialog.compoundDrawablesRelative[2].alpha = 0
        text_error.visibility = View.GONE
    }
}
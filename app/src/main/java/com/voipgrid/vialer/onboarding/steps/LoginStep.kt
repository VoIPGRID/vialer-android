package com.voipgrid.vialer.onboarding.steps

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.voipgrid.vialer.*
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.api.models.PasswordChangeParams
import com.voipgrid.vialer.fcm.RemoteMessageData
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.VoipgridLogin
import com.voipgrid.vialer.onboarding.VoipgridLogin.LoginResult
import com.voipgrid.vialer.onboarding.VoipgridLogin.LoginResult.*
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.TwoFactorFragmentHelper
import com.voipgrid.vialer.voipgrid.PasswordResetWebActivity
import kotlinx.android.synthetic.main.onboarding_step_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.android.ext.android.inject
import javax.inject.Inject

class LoginStep : Step() {

    override val layout = R.layout.onboarding_step_login

    @Inject lateinit var connectivityHelper: ConnectivityHelper
    @Inject lateinit var login: VoipgridLogin

    private val logger = Logger(this).forceRemoteLogging(true)
    private var twoFactorHelper: TwoFactorFragmentHelper? = null
    private var twoFactorDialog: AlertDialog? = null
    private var changePasswordDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)

        val enableSubmitButton: (_: Editable?) -> Unit = {
            button_login.isEnabled = emailTextDialog.length() > 0 && passwordTextDialog.length() > 0
        }

        emailTextDialog.onTextChanged(enableSubmitButton)
        passwordTextDialog.onTextChanged(enableSubmitButton)

        KeyboardVisibilityEvent.setEventListener(activity) { keyboardIsVisible ->
            if (keyboardIsVisible) {
                title_label.visibility = View.GONE
                subtitle_label.visibility = View.GONE
            } else {
                title_label.visibility = View.VISIBLE
                subtitle_label.visibility = View.VISIBLE
            }
        }

        passwordTextDialog.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent? ->
            actionId == EditorInfo.IME_ACTION_DONE && button_login.performClick()
        }

        button_login.setOnClickListenerAndDisable {
            emailTextDialog.clearFocus()
            passwordTextDialog.clearFocus()
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

        emailTextDialog.setText(intent.getStringExtra(PasswordResetWebActivity.USERNAME_EXTRA))
        passwordTextDialog.setText(intent.getStringExtra(PasswordResetWebActivity.PASSWORD_EXTRA))
        if (button_login.isEnabled) button_login.performClick()
    }

    /**
     * Attempt to log the user into VoIPGRID by launching a co-routine.
     *
     */
    private fun attemptLogin(code: String? = null) = GlobalScope.launch(Dispatchers.Main) {
        onboarding?.isLoading = true
        logger.i("Attempting to log the user into VoIPGRID, with the following 2FA code: $code")
        val result = login.attempt(emailTextDialog.text.toString(), passwordTextDialog.text.toString(), code)
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
            login.lastError?.let { error(it.title, it.description) }
            button_login.isEnabled = true
        }
        SUCCESS -> {
//            logger.i("Login to VoIPGRID was successful, progressing the user in onboarding")
//            twoFactorDialog?.dismiss()
//            onboarding?.progress(this)
            showChangePasswordDialog()
        }
        TWO_FACTOR_REQUIRED -> {
            logger.i("User logged into VoIPGRID with the correct username/password but is now required to input a valid 2FA code")
            button_login.isEnabled = true
            showTwoFactorDialog()
        }
        MUST_CHANGE_PASSWORD -> {
            logger.i("User must change their password before we can login")
            showChangePasswordDialog()
        }
    }

    /**
     * Create and show a dialog for the user to change the password.
     *
     */
    private fun showChangePasswordDialog() {
        activity?.let {
            val changePasswordDialog = AlertDialog.Builder(it)
                    .setView(R.layout.activity_create_new_password)
                    .show()

            val passwordField = changePasswordDialog.findViewById(R.id.password_field_1) as EditText

            (changePasswordDialog.findViewById(R.id.button_continue) as Button).setOnClickListener {
                val passwordChangeParams = PasswordChangeParams(
                        usersEmail = emailTextDialog.text.toString(),
                        usersCurrentPassword = passwordTextDialog.text.toString(),
                        usersNewPassword = passwordField.text.toString()
                )
                logger.e(passwordChangeParams.toString())
                GlobalScope.launch(Dispatchers.Main) {
                    if (!replyServer(passwordChangeParams)) {
                        Toast.makeText(activity, activity?.getString(R.string.password_change_failure_toast), Toast.LENGTH_LONG).show();
                    }
                }


            }

            this.changePasswordDialog = changePasswordDialog
        }
    }

    private suspend fun replyServer(passwordChangeParams: PasswordChangeParams) : Boolean = withContext(Dispatchers.IO) {
        val response = voipgridApi.passwordChange(passwordChangeParams).execute()
        return@withContext response.isSuccessful
    }




    /**
     * Create and show a dialog for the user to enter a two-factor token.
     *
     */
    private fun showTwoFactorDialog() {
        activity?.let {
            val twoFactorDialog = AlertDialog.Builder(it)
                    .setView(R.layout.onboarding_dialog_two_factor)
                    .show()

            val codeField = (twoFactorDialog.findViewById(R.id.two_factor_code_field) as EditText)
            twoFactorHelper = TwoFactorFragmentHelper(it, codeField).apply {
                focusOnTokenField()
                pasteCodeFromClipboard()
            }

            (twoFactorDialog.findViewById(R.id.button_continue) as Button).setOnClickListener {
                onboarding?.isLoading = true
                attemptLogin(codeField.text.toString())
            }

            this.twoFactorDialog = twoFactorDialog
        }
    }

    /**
     * Launches an activity to allow the user to reset their password.
     *
     */
    private fun launchForgottenPasswordActivity() {
        logger.i("Detected forgot password click, launching activity")
        ForgottenPasswordActivity.launchForEmail(onboarding as Context, emailTextDialog.text.toString())
    }
}
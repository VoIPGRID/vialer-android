package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.voipgrid.vialer.*
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.VoipgridLogin
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.onboarding.core.onTextChanged
import com.voipgrid.vialer.util.ConnectivityHelper
import kotlinx.android.synthetic.main.onboarding_step_login.*
import javax.inject.Inject

class LoginStep : Step() {

    override val layout = R.layout.onboarding_step_login

    @Inject lateinit var connectivityHelper: ConnectivityHelper
    @Inject lateinit var login: VoipgridLogin

    private val logger = Logger(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)

        val enableSubmitButton: (_: Editable?) -> Unit = {
            button_login.isEnabled = emailTextDialog.length() > 0 && passwordTextDialog.length() > 0
        }

        emailTextDialog.onTextChanged(enableSubmitButton)
        passwordTextDialog.onTextChanged(enableSubmitButton)

        passwordTextDialog.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent? ->
            actionId == EditorInfo.IME_ACTION_DONE && button_login.performClick()
        }

        button_login.setOnClickListener {
            state.username = emailTextDialog.text.toString()
            state.password = passwordTextDialog.text.toString()

            onboarding?.isLoading = true

            logger.i("Attempting login...")

            login.attempt(state.username, state.password)
        }

        button_forgot_password.setOnClickListener {
            logger.i("Detected forgot password click, launching activity")

            ForgottenPasswordActivity.launchForEmail(onboarding as Context, emailTextDialog.text.toString())
        }

        button_info.setOnClickListener {
            logger.i("Launching app info page")
            val intent = Intent(activity, WebActivity::class.java)
            intent.putExtra(WebActivity.PAGE, getString(R.string.url_app_info))
            intent.putExtra(WebActivity.TITLE, getString(R.string.info_menu_item_title))
            startActivity(intent)
        }

        login.onRequiresTwoFactor = {
            logger.i("This user requires a two-factor code to be entered")
            state.requiresTwoFactor = true
            onboarding?.progress()
        }

        login.onLoggedIn = {
            logger.i("Logged in successfully!")
            onboarding?.progress()
        }

        login.onError = { title: Int, description: Int ->
            logger.w("Error logging in $title - $description")
            error(title, description)
        }
    }
}

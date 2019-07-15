package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.voipgrid.vialer.*
import com.voipgrid.vialer.api.ApiTokenFetcher
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.util.AccountHelper
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.JsonStorage
import kotlinx.android.synthetic.main.fragment_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class LoginStep: Step(), Callback<SystemUser> {

    override val layout = R.layout.fragment_login

    @Inject lateinit var connectivityHelper: ConnectivityHelper
    @Inject lateinit var accountHelper: AccountHelper
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var jsonStorage: JsonStorage<Any>

    private val logger = Logger(this)
    private val apiTokenListener = ApiTokenFetchListener()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)

        val enableSubmitButton: (_: Editable?) -> Unit = {
            button_login.isEnabled = emailTextDialog.length() > 0 && passwordTextDialog.length() > 0
        }

        emailTextDialog.onTextChanged(enableSubmitButton)
        passwordTextDialog.onTextChanged(enableSubmitButton)

        passwordTextDialog.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent ->
            actionId == EditorInfo.IME_ACTION_DONE && button_login.performClick()
        }

        button_login.setOnClickListener {
            attemptLogin()
        }

        button_forgot_password.setOnClickListener {
            ForgottenPasswordActivity.launchForEmail(onboarding as Context, emailTextDialog.text.toString())
        }

        button_info.setOnClickListener {
            val intent = Intent(activity, WebActivity::class.java)
            intent.putExtra(WebActivity.PAGE, getString(R.string.url_app_info))
            intent.putExtra(WebActivity.TITLE, getString(R.string.info_menu_item_title))
            startActivity(intent)
        }
    }

    private fun attemptLogin() {
        ApiTokenFetcher
                .forCredentials(activity, emailTextDialog.text.toString(), passwordTextDialog.text.toString())
                .setListener(apiTokenListener)
                .fetch()
    }

    override fun onResponse(call: Call<SystemUser>, response: Response<SystemUser>) {
        if (!response.isSuccessful) {
            error(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
            return
        }

        val user = response.body() as SystemUser

        if (user.partner != null) {
            error(R.string.user_is_partner_error_title, R.string.user_is_partner_error_message)
            return
        }

        if (user.outgoingCli == null || user.outgoingCli.isEmpty()) {
            logger.d("The user does not have an outgoing cli")
        }

        preferences.setSipPermission(true)
        accountHelper.setCredentials(user.email, passwordTextDialog.text.toString())
        jsonStorage.save(user)
        onboarding?.progress()
    }

    override fun onFailure(call: Call<SystemUser>, t: Throwable) {
    }

    private inner class ApiTokenFetchListener: ApiTokenFetcher.ApiTokenListener {

        override fun twoFactorCodeRequired() {
            // go to 2 factor
        }

        override fun onSuccess(apiToken: String?) {
            accountHelper.setCredentials(emailTextDialog.text.toString(), passwordTextDialog.text.toString(), apiToken)
            voipgridApi.systemUser().enqueue(this@LoginStep)
        }

        override fun onFailure() {
            error(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
        }
    }

}

package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.voipgrid.vialer.ForgottenPasswordActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.WebActivity
import com.voipgrid.vialer.api.ApiTokenFetcher
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.util.AccountHelper
import com.voipgrid.vialer.util.ConnectivityHelper
import kotlinx.android.synthetic.main.fragment_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginStep: Step(), TextWatcher, Callback<SystemUser> {

    override val layout = R.layout.fragment_login

    private lateinit var connectivityHelper: ConnectivityHelper
    private lateinit var apiTokenFetcher: ApiTokenFetcher
    private lateinit var voipgridApi: VoipgridApi
    private lateinit var accountHelper: AccountHelper

    private val apiTokenListener = ApiTokenFetchListener()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailTextDialog.addTextChangedListener(this)
        passwordTextDialog.addTextChangedListener(this)
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
                .forCredentials(activity, emailTextDialog.toString(), passwordTextDialog.toString())
                .setListener(apiTokenListener)
                .fetch()
    }

    override fun onFailure(call: Call<SystemUser>, t: Throwable) {
    }

    override fun onResponse(call: Call<SystemUser>, response: Response<SystemUser>) {
    }

    override fun afterTextChanged(s: Editable?) {
        button_login.isEnabled = emailTextDialog.length() > 0 && emailTextDialog.length() > 0
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    private inner class ApiTokenFetchListener: ApiTokenFetcher.ApiTokenListener {
        override fun twoFactorCodeRequired() {
            // go to 2 factor
        }

        override fun onSuccess(apiToken: String?) {
            accountHelper.setCredentials(emailTextDialog.toString(), passwordTextDialog.toString(), apiToken)
            voipgridApi.systemUser().enqueue(this@LoginStep)
        }

        override fun onFailure() {
        }
    }

}
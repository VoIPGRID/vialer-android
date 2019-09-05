package com.voipgrid.vialer.onboarding

import android.content.Context
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.ApiTokenFetcher
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.logging.Logger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VoipgridLogin(private val context: Context): Callback<SystemUser>, ApiTokenFetcher.ApiTokenListener {

    private val logger = Logger(this)
    private val voipgridApi: VoipgridApi
        get() = ServiceGenerator.createApiService(context)

    var onError: ((title: Int, description: Int) -> Unit)? = null
    var onLoggedIn: (() -> Unit)? = null
    var onRequiresTwoFactor: (() -> Unit)? = null

    private var username: String? = null
    private var password: String? = null
    private var code: String? = null

    fun attempt(username: String, password: String, code: String? = null) {
        this.username = username
        this.password = password
        this.code = code

        val fetcher = ApiTokenFetcher
                .forCredentials(context, username, password)
                .setListener(this)

        if (code == null) {
            fetcher.fetch()
        } else {
            fetcher.fetch(code)
        }
    }

    override fun onResponse(call: Call<SystemUser>, response: Response<SystemUser>) {
        if (!response.isSuccessful) {
            onError?.invoke(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
            return
        }

        val user = response.body() as SystemUser

        if (user.partner != null) {
            onError?.invoke(R.string.user_is_partner_error_title, R.string.user_is_partner_error_message)
            return
        }

        if (user.outgoingCli == null || user.outgoingCli.isEmpty()) {
            logger.d("The user does not have an outgoing cli")
        }

        User.voip.isAccountSetupForSip = true
        User.voipgridUser = user
        onLoggedIn?.invoke()
    }

    override fun onFailure(call: Call<SystemUser>, t: Throwable) {
        onError?.invoke(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
    }

    override fun twoFactorCodeRequired() {
        onRequiresTwoFactor?.invoke()
    }

    override fun onSuccess(apiToken: String?) {
        if (username != null) {
            User.username = username as String
        }
        voipgridApi.systemUser().enqueue(this)
    }

    override fun onFailure() {
        onError?.invoke(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
    }
}
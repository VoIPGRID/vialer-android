package com.voipgrid.vialer.onboarding

import android.content.Context
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.api.models.ApiTokenRequest
import com.voipgrid.vialer.api.models.SystemUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VoipgridLogin(private val context: Context) {

    private val voipgrid: VoipgridApi
        get() = ServiceGenerator.createApiService(context)

    var lastError: Error? = null
        private set

    /**
     * Attempt to login to VoIPGRID, a LoginResult will be returned determining what has actually happened.
     *
     */
    suspend fun attempt(username: String, password: String, code: String? = null): LoginResult = withContext(Dispatchers.IO) {
        val response = voipgrid.apiToken(ApiTokenRequest(username, password, code)).execute()

        if (response.isSuccessful) {
            return@withContext processSuccessfulLogin(username, response.body()?.apiToken ?: "")
        }

        val errorString = response.errorBody()?.string() ?: ""

        if (code == null && responseIndicatesThat2faIsRequired(errorString)) {
            return@withContext LoginResult.TWO_FACTOR_REQUIRED
        }

        lastError = when(responseIndicatesTooManyAttempts(errorString)) {
            true -> Error(R.string.onboarding_login_failed_too_many_attempts_title, R.string.onboarding_login_failed_too_many_attempts_message)
            false -> Error(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
        }

        return@withContext LoginResult.FAIL
    }

    /**
     * After logging in we must find the actual user and make sure they are eligible to use
     * the app.
     *
     */
    private fun processSuccessfulLogin(username: String, token: String): LoginResult {
        User.username = username
        User.loginToken = token

        val response = voipgrid.systemUser().execute()

        if (!response.isSuccessful) {
            lastError = Error(R.string.onboarding_login_failed_title, R.string.onboarding_login_failed_message)
            return LoginResult.FAIL
        }

        val user = response.body() as SystemUser

        if (user.partner != null) {
            lastError = Error(R.string.user_is_partner_error_title, R.string.user_is_partner_error_message)
            return LoginResult.FAIL
        }

        User.voip.isAccountSetupForSip = true
        User.voipgridUser = user

        return LoginResult.SUCCESS
    }

    /**
     * Check the response to see if the reason for failure is due to no
     * two-factor code being provided.
     *
     * @param errorString The response body as a string
     * @return TRUE if the responses is stating the the two-factor code is missing
     */
    private fun responseIndicatesThat2faIsRequired(errorString: String): Boolean {
        return errorString.contains("two_factor_token")
    }

    /**
     * Check the response to see if the reason for failure is due to being
     * rate limited.
     *
     * @param errorString The response body as a string
     * @return TRUE if the responses is stating we are rate limited
     */
    private fun responseIndicatesTooManyAttempts(errorString: String): Boolean {
        return errorString.contains("Too many failed login attempts")
    }

    /**
     * The result returned when attempting to login to determine the subsequent actions.
     *
     */
    enum class LoginResult {
        FAIL, SUCCESS, TWO_FACTOR_REQUIRED
    }

    /**
     * A class to hold error messages regarding the login attempt, these are as resource
     * ids.
     *
     */
    data class Error(val title: Int, val description: Int)
}
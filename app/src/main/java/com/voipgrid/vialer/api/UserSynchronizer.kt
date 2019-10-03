package com.voipgrid.vialer.api

import android.content.Context
import android.util.Log
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.persistence.Middleware
import kotlinx.coroutines.*

/**
 * The responsibility of this is to make sure our local user information matches the information stored
 * in the api.
 *
 */
class UserSynchronizer(private val voipgridApi: VoipgridApi, private val context: Context, private val secureCalling: SecureCalling) {

    private val logger = Logger(this)

    /**
     * Sync our local user information with that on voipgrid.
     *
     */
    fun sync() = GlobalScope.launch(Dispatchers.Main) {
        syncVoipgridUser()

        val voipgridUser = User.voipgridUser ?: return@launch

        if (!voipgridUser.hasVoipAccount()) {
            handleMissingVoipAccount()
            return@launch
        }

        syncVoipAccount(voipgridUser)

        if (User.hasVoipAccount) {
            MiddlewareHelper.registerAtMiddleware(context)
        }
    }

    /**
     * If there is no voip account we want to make sure that our local version
     * reflects that.
     *
     */
    private fun handleMissingVoipAccount() {
        User.voipAccount = null
        User.voip.hasEnabledSip = false
        Log.e("TEST123", " Is voip account null? " + (User.voipAccount == null))
    }

    /**
     * Fetch the voipgrid user from the api and update our stored version.
     *
     */
    private suspend fun syncVoipgridUser() = withContext(Dispatchers.IO) {
        val response = voipgridApi.systemUser().execute()

        if (!response.isSuccessful) {
            logger.e("Unable to retrieve a system user....")
            return@withContext
        }

        User.voipgridUser = response.body()
        User.voip.isAccountSetupForSip = true
    }

    /**
     * Sync the voip account with the remote version.
     *
     */
    private suspend fun syncVoipAccount(voipgridUser: SystemUser) = withContext(Dispatchers.IO)  {
        val response = voipgridApi.phoneAccount(voipgridUser.voipAccountId).execute()

        if (!response.isSuccessful) return@withContext

        val voipAccount = response.body() ?: return@withContext

        if (voipAccount.accountId == null) {
            handleMissingVoipAccount()
            return@withContext
        }

        User.voipAccount = response.body()
        secureCalling.updateApiBasedOnCurrentPreferenceSetting()
    }
}
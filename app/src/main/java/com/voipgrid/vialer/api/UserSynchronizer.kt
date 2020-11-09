package com.voipgrid.vialer.api

import android.content.Context
import android.util.Log
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.middleware.Middleware
import com.voipgrid.vialer.persistence.VoipSettings
import kotlinx.coroutines.*

/**
 * The responsibility of this is to make sure our local user information matches the information stored
 * in the api.
 *
 */
class UserSynchronizer(private val voipgridApi: VoipgridApi, private val secureCalling: SecureCalling, private val middleware: Middleware) {

    private val logger = Logger(this)

    /**
     * Sync our local user information with that on voipgrid.
     *
     */
    suspend fun sync() {
        try {
            syncVoipgridUser()

            val voipgridUser = User.voipgridUser ?: return

            syncUserDestinations()

            if (!voipgridUser.hasVoipAccount()) {
                handleMissingVoipAccount()
                return
            }

            syncVoipAccount(voipgridUser)

            if (User.hasVoipAccount) {
                secureCalling.updateApiBasedOnCurrentPreferenceSetting()
                middleware.refresh()
            }
        } catch (e: Exception) {
            logger.w("Failed to sync user ${e.message}")
        }
    }

    /**
     * Temporary method for java interop, to be removed after SettingsActivity has been
     * reverted to Kotlin.
     *
     */
    fun syncWithCallback(dispatcher: CoroutineDispatcher = Dispatchers.IO, callback: () -> Unit) = GlobalScope.launch(dispatcher) {
        sync()
        callback.invoke()
    }

    /**
     * If there is no voip account we want to make sure that our local version
     * reflects that.
     *
     */
    private fun handleMissingVoipAccount() {
        logger.i("User does not have a VoIP account, disabling sip")
        User.voipAccount = null
        User.voip.hasEnabledSip = false
    }

    /**
     * Fetch the voipgrid user from the api and update our stored version.
     *
     */
    private suspend fun syncVoipgridUser() = withContext(Dispatchers.IO) {
        val response = voipgridApi.systemUser().execute()

        if (!response.isSuccessful) {
            logger.e("Unable to retrieve a system user")
            return@withContext
        }

        User.voipgridUser = response.body()
        User.voip.isAccountSetupForSip = true
        User.uuid = User.voipgridUser?.uuid ?: ""
    }

    /**
     * Sync the voip account with the remote version.
     *
     */
    private suspend fun syncVoipAccount(voipgridUser: SystemUser) = withContext(Dispatchers.IO)  {
        val response = voipgridApi.phoneAccount(voipgridUser.voipAccountId).execute()

        if (!response.isSuccessful) {
            logger.e("Unable to retrieve voip account from api")
            return@withContext
        }

        val voipAccount = response.body() ?: return@withContext

        if (voipAccount.accountId == null) {
            handleMissingVoipAccount()
            return@withContext
        }

        User.voipAccount = response.body()
    }

    private suspend fun syncUserDestinations() = withContext(Dispatchers.IO) {
        val response = voipgridApi.fetchDestinations().execute()

        if (!response.isSuccessful) {
            logger.e("Unable to retrieve sync user destinations: " + response.code())
            return@withContext
        }

        val body = response.body() ?: return@withContext

        val destinations = body.objects ?: return@withContext

        val userDestination = destinations.first() ?: return@withContext

        User.internal.destinations = destinations

        val currentDestinationId = userDestination.selectUserDestination.fixedDestinationId
                ?: userDestination.selectUserDestination.phoneAccountId

        if (currentDestinationId == null) {
            User.voip.availability = VoipSettings.Availability.NOT_AVAILABLE
        }
        else if (currentDestinationId == User.voipgridUser?.voipAccountId) {
            User.voip.availability = VoipSettings.Availability.AVAILABLE
        }
        else {
            User.voip.availability = VoipSettings.Availability.ELSEWHERE
        }
    }
}
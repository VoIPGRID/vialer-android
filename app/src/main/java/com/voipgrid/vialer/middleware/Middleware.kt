package com.voipgrid.vialer.middleware

import android.content.Context
import android.os.Build
import com.google.firebase.iid.FirebaseInstanceId
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.persistence.VoipSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Middleware(private val context: Context) {

    private val logger = Logger(this)

    private val api by lazy { ServiceGenerator.createRegistrationService(context) }

    /**
     * Register the current token with the middleware, as long as the user is valid
     * a request will always be sent to the server.
     *
     */
    fun register() {
        if (!isValidUserToRegisterWithMiddleware()) return

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                logger.w("Unable to get the token")
                return@addOnCompleteListener
            }

            val token = task.result?.token ?: return@addOnCompleteListener

            GlobalScope.launch {
                try {
                    performRegistrationApiRequest(token)
                    logger.i("Registered at the middleware with the token: $token")
                } catch (e: Exception) {
                    logger.w("Failed to register ${e.message}")
                }
            }
        }
    }

    /**
     * Unregister from the middleware, the app will no longer receive incoming calls
     * until a call to register() is made.
     *
     */
    fun unregister() {
        if (User.middleware.currentToken.isBlank()) {
            logger.w("Not unregistering as we do not have a token")
            return
        }

        GlobalScope.launch {
            try {
                performUnregisterApiRequest()
                logger.i("Unregistered from the middleware successfully")
            } catch (e: Exception) {
                logger.w("Failed to unregister ${e.message}")
            }
        }
    }

    /**
     * Refresh middleware status, either registering or unregistering based on the
     * current user.
     *
     */
    fun refresh() = if (isValidUserToRegisterWithMiddleware()) {
        register()
    } else {
        unregister()
    }

    private fun isValidUserToRegisterWithMiddleware(): Boolean = User.isLoggedIn && User.voip.canUseSip && User.voip.availability != VoipSettings.Availability.DND

    private suspend fun performRegistrationApiRequest(token: String) = withContext(Dispatchers.IO) {
        val response = api.register(
                User.voipgridUser?.fullName,
                token,
                User.voipAccount?.accountId,
                Build.VERSION.CODENAME,
                Build.VERSION.RELEASE,
                context.packageName,
                if (User.userPreferences.remoteLoggingIsEnabled) User.uuid else null
        ).execute()

        if (response.isSuccessful) {
            User.middleware.currentToken = token
        } else {
            throw Exception("Failed to unregister from the middleware, ${response.code()}")
        }
    }

    private suspend fun performUnregisterApiRequest() = withContext(Dispatchers.IO) {
        val response = api.unregister(
                User.middleware.currentToken,
                User.voipAccount?.accountId,
                context.packageName
        ).execute()

        if (response.isSuccessful) {
            User.middleware.currentToken = ""
        } else {
            throw Exception("Failed to register with the middleware, ${response.code()}")
        }
    }
}
package com.voipgrid.vialer.voip.middleware

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.MiddlewareApi
import com.voipgrid.vialer.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call

class Middleware(private val api: MiddlewareApi, private val user: User, private val context: Context) {

    private val logger = Logger(this)

    /**
     * Register with the middleware, if no token is provided, the current token will be used.
     *
     */
    @JvmOverloads fun register(token: String = user.middleware.currentToken) {
        if (!user.voip.canUseSip || !user.isLoggedIn || !user.hasVoipAccount) {
            logger.i("Not registering for the middleware as this user account is not setup correctly")
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            makeRegisterRequest(token)
        }
    }

    /**
     * Make the request to register with the middleware.
     *
     */
    private suspend fun makeRegisterRequest(token: String) = withContext(Dispatchers.IO) {
        Log.e("TEST123", "Attempting to register with $token")
        val response = createRegisterRequest(token).execute()

        if (!response.isSuccessful) {
            logger.e("Registration with the middleware failed")
            return@withContext
        }
Log.e("TEST123", "Registration successss")
        User.middleware.currentToken = token
    }

    /**
     * Unregister from the middleware.
     *
     */
    @JvmOverloads fun unregister(token: String = user.middleware.currentToken) {
        val voipAccountId = user.voipAccount?.accountId ?: return

        GlobalScope.launch(Dispatchers.Main) {
            api.unregister(token, voipAccountId, context.packageName).execute()
        }
    }

    /**
     * Reply to a request from the middleware to handle an incoming call.
     *
     */
    fun replyToIncomingCall(pushMessage: RemoteMessage, isAvailable: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            api.reply(pushMessage.data["unique_key"], isAvailable, pushMessage.data["message_start_time"]).execute()
        }
    }

    /**
     * Create the api request to pass our token to the middleware.
     *
     */
    private fun createRegisterRequest(token: String): Call<ResponseBody> = api.register(
            user.voipgridUser?.fullName,
            token,
            user.voipAccount?.accountId,
            Build.VERSION.CODENAME,
            Build.VERSION.RELEASE,
            context.packageName, if (user.remoteLogging.isEnabled) user.remoteLogging.id else null
    )
}
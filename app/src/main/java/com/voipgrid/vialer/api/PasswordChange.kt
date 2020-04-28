package com.voipgrid.vialer.api

import com.voipgrid.vialer.api.models.PasswordChangeParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception


class PasswordChange(private val api: VoipgridApi) {

    /**
     * Perform the password change and then return the relevant result.
     *
     */
    suspend fun perform(email: String, currentPassword: String, newPassword: String): Result = withContext(Dispatchers.IO) {
        try {
            val response = api.passwordChange(PasswordChangeParams(email, currentPassword, newPassword)).execute()

            if (!response.isSuccessful) {
                return@withContext Result.FAIL
            }

            return@withContext Result.SUCCESS
        } catch (e: Exception) {
            Result.FAIL
        }
    }

    /**
     * The possible results that can occur when performing a password change.
     *
     */
    enum class Result {
        SUCCESS, FAIL
    }
}
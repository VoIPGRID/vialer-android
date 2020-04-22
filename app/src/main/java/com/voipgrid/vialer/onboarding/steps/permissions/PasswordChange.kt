package com.voipgrid.vialer.onboarding.steps.permissions
package com.voipgrid.vialer.api

import android.widget.EditText
import android.widget.Toast
import com.voipgrid.vialer.R
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.api.models.PasswordChangeParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


    ​
    class PasswordChange(private val api: VoipgridApi) {
        ​
        /**
         * Perform the password change and then return the relevant result.
         *
         */
        suspend fun perform(userseEail: String, usersCurrentPassword: String, usersNewPassword: String): Result = withContext(Dispatchers.IO) {
            ​logger.e(passwordChangeParams.toString())
            GlobalScope.launch(Dispatchers.Main) {
                if (!replyServer(passwordChangeParams)) {
                    Toast.makeText(activity, activity?.getString(R.string.password_change_failure_toast), Toast.LENGTH_LONG).show();
                }
            }
        }

                ​
                /**
                 * The possible results that can occur when performing a password change.
                 *
                 */
                enum class Result {
                    ​
                    private suspend fun replyServer(passwordChangeParams: PasswordChangeParams): Boolean = withContext(Dispatchers.IO) {
                        val response = voipgridApi.passwordChange(passwordChangeParams).execute()
                        return@withContext response.isSuccessful
                    }
                }
            }
        }
    }
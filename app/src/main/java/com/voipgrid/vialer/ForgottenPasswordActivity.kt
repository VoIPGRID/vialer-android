package com.voipgrid.vialer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.models.PasswordResetParams
import com.voipgrid.vialer.onboarding.core.onTextChanged
import kotlinx.android.synthetic.main.activity_forgotten_password.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ForgottenPasswordActivity: AppCompatActivity(), View.OnClickListener {

    @Inject lateinit var voipgridApi: VoipgridApi

    private val logger = Logger(this)
    private val email: String
        get() = forgotPasswordEmailTextDialog.text.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgotten_password)
        VialerApplication.get().component().inject(this)

        forgotPasswordEmailTextDialog.setText(intent.getStringExtra(EMAIL_EXTRA) ?: "")
        button_send_password_email.setOnClickListener(this)


        forgotPasswordEmailTextDialog.onTextChanged {
            s -> button_send_password_email.isEnabled = s?.isNotEmpty() ?: false
        }
    }

    override fun onClick(view: View?) {
        if (forgotPasswordEmailTextDialog.toString().isEmpty()) return

        AlertDialog.Builder(this)
                .setTitle(getString(R.string.forgot_password_alert_title))
                .setMessage(getString(R.string.forgot_password_alert_message, email))
                .setCancelable(false)
                .setPositiveButton(this.getString(R.string.ok)) { _, _ -> resetPassword(email) }
                .setNegativeButton(this.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .create().show()
    }

    private fun resetPassword(email: String) {
        val call = voipgridApi.resetPassword(PasswordResetParams(email))
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    logger.e("Password reset request failed with code: ${response.code()}")
                    failedToResetPassword()
                    return
                }

                AlertDialog.Builder(this@ForgottenPasswordActivity)
                        .setTitle(R.string.forgot_password_success_title)
                        .setMessage(R.string.forgot_password_success_message)
                        .setPositiveButton(R.string.ok) { dialogInterface, _ ->
                            dialogInterface.dismiss()
                            finish()
                        }
                        .create()
                        .show()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                logger.e("Password reset request failed with error: ${t.message}")
                failedToResetPassword()
            }
        })
    }

    fun failedToResetPassword() {
        AlertDialog.Builder(this@ForgottenPasswordActivity)
                .setTitle(R.string.forgot_password_failed_title)
                .setMessage(R.string.forgot_password_failed_message)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
    }

    companion object {
        private const val EMAIL_EXTRA = "com.voipgrid.vialer.ForgottenPasswordActivity.EMAIL"

        /**
         * Launch the forgotten password activity with an email address to use
         * as the default in the field
         *
         */
        fun launchForEmail(context: Context, email: String) {
            val intent = Intent(context, ForgottenPasswordActivity::class.java).apply {
                putExtra(EMAIL_EXTRA, email)
                flags = FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
        }
    }
}
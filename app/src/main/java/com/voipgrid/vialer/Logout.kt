package com.voipgrid.vialer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.util.ConnectivityHelper

class Logout(private val context: Context, private val sharedPreferences: SharedPreferences, private val connectivityHelper: ConnectivityHelper) {

    fun perform(force: Boolean = false) {
        if (!connectivityHelper.hasNetworkConnection() && !force) {
            showErrorDialog()
            return
        }

        if (connectivityHelper.hasNetworkConnection()) {
            MiddlewareHelper.unregister(context)
        }

        User.clear()
        sharedPreferences.edit().clear().apply()

        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(context)
                .setTitle(R.string.cannot_logout_error_title)
                .setMessage(R.string.cannot_logout_error_text)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
    }
}
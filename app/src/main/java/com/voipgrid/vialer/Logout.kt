package com.voipgrid.vialer

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.util.AccountHelper
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.JsonStorage

class Logout(private val context: Context, private val jsonStorage: JsonStorage<Any>, private val accountHelper: AccountHelper, private val sharedPreferences: SharedPreferences, private val connectivityHelper: ConnectivityHelper) {

    fun perform(force: Boolean = false) {
        if (!connectivityHelper.hasNetworkConnection() && !force) {
            showErrorDialog()
            return
        }

        if (connectivityHelper.hasNetworkConnection()) {
            MiddlewareHelper.unregister(context)
        }

        jsonStorage.clear()
        accountHelper.clearCredentials()
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
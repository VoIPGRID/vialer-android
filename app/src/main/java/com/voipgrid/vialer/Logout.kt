package com.voipgrid.vialer

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import com.voipgrid.vialer.onboarding.Onboarder
import com.voipgrid.vialer.util.ConnectivityHelper
import nl.voipgrid.vialer_voip.middleware.Middleware

class Logout(private val context: Context, private val sharedPreferences: SharedPreferences, private val connectivityHelper: ConnectivityHelper, private val middleware: Middleware) {

    /**
     * Log the currently logged in user out, performing all the tasks we need to perform before they can be logged out.
     *
     */
    fun perform(force: Boolean = false, activity: Activity?) {
        if (!connectivityHelper.hasNetworkConnection() && !force) {
            showErrorDialog()
            return
        }

        if (connectivityHelper.hasNetworkConnection()) {
            middleware.unregister()
        }

        User.clear()
        sharedPreferences.edit().clear().apply()

        activity?.let { Onboarder.start(it) }
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
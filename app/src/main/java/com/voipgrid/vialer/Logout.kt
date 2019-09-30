package com.voipgrid.vialer

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.onboarding.Onboarder
import com.voipgrid.vialer.util.ConnectivityHelper

class Logout(private val context: Context, private val sharedPreferences: SharedPreferences, private val connectivityHelper: ConnectivityHelper) {

    fun perform(force: Boolean = false, activity: Activity) {
        if (!connectivityHelper.hasNetworkConnection() && !force) {
            showErrorDialog()
            return
        }

        if (connectivityHelper.hasNetworkConnection()) {
            MiddlewareHelper.unregister(context)
        }

        User.clear()
        sharedPreferences.edit().clear().apply()

        Onboarder.start(activity)
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
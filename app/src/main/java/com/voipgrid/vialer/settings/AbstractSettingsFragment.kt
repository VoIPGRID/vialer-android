package com.voipgrid.vialer.settings

import android.app.AlertDialog
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.api.VoipgridApi
import kotlinx.android.synthetic.main.activity_settings.*
import org.koin.android.ext.android.inject

abstract class AbstractSettingsFragment : PreferenceFragmentCompat() {

    protected val userSynchronizer: UserSynchronizer by inject()

    protected val voipgridApi: VoipgridApi by inject()

    /**
     * When set to TRUE the UI will show a loading progress bar.
     *
     */
    var isLoading: Boolean
        get() = (activity as SettingsActivity).loading.visibility == VISIBLE
        set(loading) {
            (activity as SettingsActivity).loading.visibility = if (loading) VISIBLE else GONE
        }

    /**
     * Alert the user with an alert dialog. This will automatically be run on the UI thread.
     *
     */
    fun alert(title: Int, description: Int) = activity?.runOnUiThread {
        isLoading = false
        AlertDialog.Builder(activity)
                .setTitle(activity?.getString(title))
                .setMessage(activity?.getString(description))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
    }

    /**
     * Refresh the summary provider that is currently assigned to this preference.
     *
     */
    fun Preference.refreshSummary() {
        summaryProvider = summaryProvider
    }

    /**
     * Extension function to set a change listener more cleanly, and specify if an error
     * should be thrown if there is no connection.
     *
     * @param networkConnectivityRequired If set to TRUE, a network connectivity check will be
     * performed first, if it fails an error dialog will be shown and the setting will
     * not be changed.
     */
    inline fun <reified T : Any> Preference.setOnChangeListener(networkConnectivityRequired: Boolean = false, crossinline listener: (value: T) -> Boolean) {
        if (activity == null) return

        setOnPreferenceChangeListener { _: Preference, value: Any ->
            if (!(activity as SettingsActivity).isConnectedToNetwork() && networkConnectivityRequired) {
                alert(R.string.no_network_connection, R.string.settings_no_internet)
                return@setOnPreferenceChangeListener false
            }

            if (value is T) {
                listener.invoke(value)
            } else {
                false
            }
        }
    }
}
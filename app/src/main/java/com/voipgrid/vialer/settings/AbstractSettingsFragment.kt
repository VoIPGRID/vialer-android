package com.voipgrid.vialer.settings

import android.app.AlertDialog
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.activity_settings.*

abstract class AbstractSettingsFragment : PreferenceFragmentCompat() {

    var isLoading: Boolean
        get() = (activity as SettingsActivity).loading.visibility == VISIBLE
        set(loading) {
            (activity as SettingsActivity).loading.visibility = if (loading) VISIBLE else GONE
        }

    fun alert(title: Int, description: Int) {
        AlertDialog.Builder(activity)
                .setTitle(activity?.getString(title))
                .setMessage(activity?.getString(description))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
    }
}
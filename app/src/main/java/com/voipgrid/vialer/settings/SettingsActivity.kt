package com.voipgrid.vialer.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.LoginRequiredActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : LoginRequiredActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    var onActivityResultCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById(R.id.action_bar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = getString(R.string.title_activity_account)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()

        supportFragmentManager.addOnBackStackChangedListener {
            loading.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val hadBackStack = supportFragmentManager.popBackStackImmediate()
        supportActionBar?.title = getString(R.string.title_activity_account)
        return hadBackStack
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        loading.visibility = View.GONE
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment).apply {
            arguments = pref.extras
            setTargetFragment(caller, 0)
        }
        supportActionBar?.title = pref.title
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResultCallback?.invoke()
    }
}
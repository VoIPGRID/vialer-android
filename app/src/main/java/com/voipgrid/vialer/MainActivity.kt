package com.voipgrid.vialer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.callrecord.CallRecordFragment
import com.voipgrid.vialer.callrecord.CallRecordFragmentHolder
import com.voipgrid.vialer.callrecord.CallRecordViewModel
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.contacts.ContactsFragment
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.options.OptionsFragment
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.LoginRequiredActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : LoginRequiredActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val defaultFragment = Fragment.Contacts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
        setContentView(R.layout.activity_main)
        bottom_nav.setOnNavigationItemSelectedListener(this)

        if (!userIsOnboarded()) {
            logger.i("User has not properly onboarded, logging them out")
            returnUserToLoginScreen()
            return
        }

        syncUser()

//        lifecycle.addObserver(RatingPopupListener(this))

        floating_action_button?.setOnClickListener { openDialer() }
    }

    /**
     * Immediately load all our fragments into our container.
     *
     */
    private fun loadFragments() {
        val fragments = arrayOf(Fragment.Contacts, Fragment.CallRecords, Fragment.Options)

        fragments.filterNot {
            supportFragmentManager.fragments.contains(it.fragment as androidx.fragment.app.Fragment)
        }.forEach {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.container, it.fragment as androidx.fragment.app.Fragment)
                hide(it.fragment as androidx.fragment.app.Fragment)
            }.commitNow()
        }

        val activeFragment = activeFragment

        swapFragment(when {
            intent.hasExtra(Extra.FRAGMENT.name) -> {
                Fragment.valueOf(intent.getStringExtra(Extra.FRAGMENT.name) ?: defaultFragment.name)
            }
            activeFragment != null -> activeFragment
            else -> defaultFragment
        })
    }

    override fun onResume() {
        super.onResume()

        // We currently only support a single call so any time this activity is opened, we will
        // request the SipService to display the current call. If there is no current call, this will have no
        // affect.
        SipService.performActionOnSipService(this, SipService.Actions.DISPLAY_CALL_IF_AVAILABLE)

        loadFragments()
    }

    /**
     * Updates the user and voip account information from the api.
     *
     */
    private fun syncUser() = GlobalScope.launch(Dispatchers.Main) {
        if (!isConnectedToNetwork()) return@launch

        userSynchronizer.sync()
    }

    /**
     * End immediately and return to the onboarding screen to let tthe user login
     * again.
     *
     */
    private fun returnUserToLoginScreen() {
        logout.perform(true, this)
        finish()
    }

    /**
     * Check that the user has fully completed the onboarding process. If they have not,
     * we will redirect them to login.
     *
     */
    private fun userIsOnboarded(): Boolean {
        return User.isLoggedIn && User.voipgridUser?.mobileNumber != null && User.internal.hasCompletedOnBoarding
    }

    /**
     * Is called when a user selects a tab in the bottom navigation view. Will load the
     * corresponding fragment into the container.
     *
     */
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        swapFragment(when (menuItem.itemId) {
            R.id.navigation_item_contacts -> Fragment.Contacts
            R.id.navigation_item_recent -> Fragment.CallRecords
            R.id.navigation_item_options -> Fragment.Options
            else -> throw IllegalArgumentException("No fragment")
        })

        return false
    }

    private fun swapFragment(fragment: Fragment) {
        val menuItemIndex = when(fragment) {
            Fragment.Contacts -> 0
            Fragment.CallRecords -> 1
            Fragment.Options -> 2
        }

        bottom_nav.menu[menuItemIndex].isChecked = true
        val mainActivityFragment = fragment.fragment

        val transaction = supportFragmentManager.beginTransaction()

        activeFragment?.let {
            transaction.hide(it.fragment as androidx.fragment.app.Fragment)
        }

        transaction.show(mainActivityFragment as androidx.fragment.app.Fragment).commit()
        floating_action_button.visibility = if (mainActivityFragment.shouldRenderDialerButton) View.VISIBLE else View.GONE
        activeFragment = fragment

        activeFragment?.let {
            if (it.fragment is CallRecordFragmentHolder) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
            }
        }
    }

    /**
     * Show the dialer view
     */
    private fun openDialer() {
        startActivity(
                Intent(this, DialerActivity::class.java),
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        floating_action_button,
                        "floating_action_button_transition_name"
                ).toBundle()
        )
    }

    interface MainActivityFragment {
        val shouldRenderDialerButton: Boolean
            get() = true
    }

    enum class Fragment(val fragment: MainActivityFragment) {
        Contacts(ContactsFragment()), CallRecords(CallRecordFragmentHolder()), Options(OptionsFragment())
    }

    enum class Extra {
        FRAGMENT
    }

    companion object {
        private var activeFragment: Fragment? = null
    }
}
package com.voipgrid.vialer

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.callrecord.AllCallsFragment
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.options.OptionsFragment
import com.voipgrid.vialer.reachability.ReachabilityReceiver
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.LoginRequiredActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : LoginRequiredActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener {

    @Inject lateinit var reachabilityReceiver: ReachabilityReceiver

    @Inject lateinit var userSynchronizer: UserSynchronizer

    private var connectivityListener: ConnectivityListener? = null

    private var currentNav: Int = 0

    override val logger = Logger(this)

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
        lifecycle.addObserver(RatingPopupListener(this))
    }

    override fun onResume() {
        super.onResume()
        reachabilityReceiver.startListening()

        // We currently only support a single call so any time this activity is opened, we will
        // request the SipService to display the current call. If there is no current call, this will have no
        // affect.
        SipService.performActionOnSipService(this, SipService.Actions.DISPLAY_CALL_IF_AVAILABLE)
    }

    override fun onPause() {
        super.onPause()
        reachabilityReceiver.stopListening()
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
        val itemId = menuItem.itemId
        if (currentNav == itemId) return false
        menuItem.isChecked = true
        currentNav = itemId

        when (itemId) {
            // R.id.navigation_item_contacts -> // TODO: Create ContactFragment
            R.id.navigation_item_recent -> supportFragmentManager.beginTransaction().replace(R.id.container, AllCallsFragment()).commit()
            R.id.navigation_item_options -> supportFragmentManager.beginTransaction().replace(R.id.container, OptionsFragment()).commit()
        }
        return false
    }

    override fun onInternetConnectivityGained() {
        connectivityListener?.onInternetConnectivityChanged()
    }

    override fun onInternetConnectivityLost() {
        connectivityListener?.onInternetConnectivityChanged()
    }

    /**
     * Listener interface to listen to changes in connectivity.
     *
     */
    interface ConnectivityListener {
        fun onInternetConnectivityChanged()
    }

    fun setConnectivityListener(connectivityListener: ConnectivityListener) {
        this.connectivityListener = connectivityListener
    }
}
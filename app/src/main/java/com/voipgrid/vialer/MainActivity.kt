package com.voipgrid.vialer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.LoginRequiredActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : LoginRequiredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
        setContentView(R.layout.activity_main)

        if (!userIsOnboarded()) {
            logger.i("User has not properly onboarded, logging them out")
            returnUserToLoginScreen()
            return
        }

        syncUser()

//        lifecycle.addObserver(RatingPopupListener(this))

        floating_action_button?.setOnClickListener { openDialer() }

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        if (intent.hasExtra(Extra.NAVIGATE_TO.name)) {
            findNavController(R.id.nav_host_fragment).navigate(intent.getIntExtra(Extra.NAVIGATE_TO.name, 0))
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            floating_action_button.visibility = if (destination.id == R.id.navigation_options) View.INVISIBLE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        // We currently only support a single call so any time this activity is opened, we will
        // request the SipService to display the current call. If there is no current call, this will have no
        // affect.
        SipService.performActionOnSipService(this, SipService.Actions.DISPLAY_CALL_IF_AVAILABLE)
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

    enum class Extra {
        NAVIGATE_TO
    }
}
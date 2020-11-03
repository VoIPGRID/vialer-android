package com.voipgrid.vialer.logging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.voipgrid.vialer.Logout
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.ConnectivityHelper
import dagger.android.AndroidInjection.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import javax.inject.Inject


abstract class VialerBaseActivity : AppCompatActivity(), KoinComponent {

    protected open val logger = Logger(this)

    val broadcastReceiverManager: BroadcastReceiverManager by inject()
    @Inject protected lateinit var connectivityHelper: ConnectivityHelper
    @Inject protected lateinit var connectivityManager: ConnectivityManager
    @Inject protected lateinit var logout: Logout

    private val networkChangeReceiver = NetworkChangeReceiver()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(RemoteUncaughtExceptionHandler())
        VialerApplication.get().component().inject(this)
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = NetworkCallback().also {
                connectivityManager.registerDefaultNetworkCallback(it)
            }
        } else {
            broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(networkChangeReceiver, CONNECTIVITY_ACTION)
        }
    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } else {
            broadcastReceiverManager.unregisterReceiver(networkChangeReceiver)
        }
    }

    protected fun hideKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        if (currentFocus != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    /**
     * Log the current user out.
     *
     */
    fun logout(force: Boolean = false) {
        logout.perform(force, this)
    }

    /**
     * Check if all permissions are granted
     *
     * @see android.app.Activity.onRequestPermissionsResult
     * @param permissions
     * @param grantResults
     * @return
     */
    fun allPermissionsGranted(permissions: Array<String>, grantResults: IntArray): Boolean {
        var allPermissionsGranted = true
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false
                break
            }
        }
        return allPermissionsGranted
    }

    /**
     * Called when internet connectivity is lost, this will also send when it is a sticky broadcast
     * so when the activity is first loaded.
     *
     */
    protected open fun onInternetConnectivityLost() {}

    /**
     * Called when internet connectivity is gained, this will also send when it is a sticky broadcast
     * so when the activity is first loaded.
     *
     */
    protected open fun onInternetConnectivityGained() {}

    /**
     * Check the current status of internet connectivity.
     *
     * @return
     */
    fun isConnectedToNetwork() = connectivityHelper.hasNetworkConnection()

    /**
     * This is a network callback for the current recommended method of detecting
     * network changes.
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private inner class NetworkCallback : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            runOnUiThread { this@VialerBaseActivity.onInternetConnectivityGained() }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            runOnUiThread { this@VialerBaseActivity.onInternetConnectivityLost() }
        }
    }

    /**
     * This is an out-dated method of detecting network changes and should be
     * removed when we no longer need to support these versions.
     *
     */
    private inner class NetworkChangeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getBooleanExtra(EXTRA_NO_CONNECTIVITY, false)) {
                onInternetConnectivityLost()
            } else {
                onInternetConnectivityGained()
            }
        }
    }

}
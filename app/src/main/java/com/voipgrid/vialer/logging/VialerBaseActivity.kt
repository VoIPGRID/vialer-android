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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.InternetConnectivity
import javax.inject.Inject


abstract class VialerBaseActivity : AppCompatActivity() {

    protected open val logger = Logger(this)

    @Inject protected lateinit var broadcastReceiverManager: BroadcastReceiverManager
    @Inject protected lateinit var connectivityHelper: ConnectivityHelper
    @Inject protected lateinit var connectivityManager: ConnectivityManager

    private val networkChangeReceiver = NetworkChangeReceiver()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    protected val internetConnectivity = InternetConnectivity()

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
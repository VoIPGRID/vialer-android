package com.voipgrid.vialer.warnings

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.util.ConnectivityHelper
import kotlinx.android.synthetic.main.fragment_warnings.*
import kotlinx.android.synthetic.main.view_number_input.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class WarningFragment : Fragment(), KoinComponent {

    private val connectivityHelper: ConnectivityHelper by inject()
    private val connectivityManager: ConnectivityManager by inject()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_warnings, container, false)
    }

    override fun onResume() {
        super.onResume()

        refresh()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = NetworkCallback().also { connectivityManager.registerDefaultNetworkCallback(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }

    private fun refresh() {
        activity?.runOnUiThread {
            voip_disabled?.visibility = if (User.voip.hasEnabledSip) View.GONE else View.VISIBLE
            not_encrypted?.visibility = if (User.voip.hasTlsEnabled) View.GONE else View.VISIBLE
            no_internet_connection?.visibility = if (connectivityHelper.hasNetworkConnection()) View.GONE else View.VISIBLE
        }
    }

    /**
     * This is a network callback for the current recommended method of detecting
     * network changes.
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private inner class NetworkCallback : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            refresh()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            refresh()
        }
    }
}
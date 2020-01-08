package com.voipgrid.vialer.options

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.voipgrid.vialer.*
import com.voipgrid.vialer.User.voipgridUser
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.api.models.Destination
import com.voipgrid.vialer.api.models.FixedDestination
import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.util.ConnectivityHelper
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.fragment_options.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class OptionsFragment : Fragment(), Callback<Any>, OnItemSelectedListener, NavigationView.OnNavigationItemSelectedListener, MainActivity.ConnectivityListener {

    private lateinit var layout: View
    private var mConnectivityHelper: ConnectivityHelper? = null
    private var spinnerAdapter: CustomFontSpinnerAdapter<Destination?>? = null

    @Inject
    lateinit var userSynchronizer: UserSynchronizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
        mConnectivityHelper = ConnectivityHelper(
                activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?,
                activity?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        )
        refreshCurrentAvailability()
        setConnectivityListener()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        layout = inflater.inflate(R.layout.fragment_options, null)
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigation_view.setNavigationItemSelectedListener(this)

        // Set the email address and phone number of the logged in user.
        setSystemUserInfo()

        // Set which version of the app the user is using.
        setVersionInfo(text_view_version)

        // Setup the spinner in the drawer.
        setupSpinner()
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentAvailability()
    }

    /**
     * Perform a request to refresh the current availability stats. This happens asynchronously.
     *
     */
    private fun refreshCurrentAvailability() {
        if (voipgridUser == null || activity !is MainActivity) {
            return
        }
        if (!(activity as MainActivity).isConnectedToNetwork()) return
        userSynchronizer.syncWithCallback {
            activity?.runOnUiThread { refresh() }
        }
    }

    private fun setSystemUserInfo() {
        val voipgridUserNotNull = voipgridUser ?: return
        val phoneNumber = voipgridUserNotNull.outgoingCli
        if (!TextUtils.isEmpty(phoneNumber)) {
            header.text_view_name.text = phoneNumber
        }
        val email = voipgridUserNotNull.email
        if (!TextUtils.isEmpty(email)) {
            header.text_view_email.text = email
        }
    }

    private fun setVersionInfo(textView: TextView?) {
        if (textView != null) {
            try {
                val packageInfo = activity?.packageManager?.getPackageInfo(activity?.packageName, 0)
                val version = packageInfo?.versionName
                if (version != null && version.contains("beta")) {
                    textView.text = getString(R.string.version_info_beta, version, packageInfo.versionCode.toString())
                } else {
                    textView.text = getString(R.string.version_info, version)
                }
                textView.visibility = View.VISIBLE
            } catch (e: PackageManager.NameNotFoundException) {
                textView.visibility = View.GONE
            }
        }
    }

    private fun setConnectivityListener() {
        if (activity !is MainActivity) return
        (activity as MainActivity).setConnectivityListener(this)
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.navigation_item_statistics -> VoIPGRIDPortalWebActivity.launchForStats(context)
            R.id.navigation_item_dial_plan -> VoIPGRIDPortalWebActivity.launchForDialPlan(context)
            R.id.navigation_item_info -> VoIPGRIDPortalWebActivity.launchForAppInfo(context)
            R.id.navigation_item_settings -> startActivity(Intent(context, SettingsActivity::class.java))
            R.id.navigation_item_logout -> promptForLogout()
        }
        return false
    }

    private fun promptForLogout() {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(this.getString(R.string.logout_dialog_text))
        builder.setPositiveButton(this.getString(R.string.logout_dialog_positive)) { _: DialogInterface?, _: Int ->
            if (activity is MainActivity)
                (activity as MainActivity).logout(false)
        }
        builder.setNegativeButton(this.getString(R.string.logout_dialog_negative)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onFailure(call: Call<Any>, t: Throwable) {}

    private fun refresh() {
        if (header.menu_availability_spinner != null && activity is MainActivity) {
            header.menu_availability_spinner.isEnabled = (activity as MainActivity).isConnectedToNetwork()
        }
        val userDestinationObjects = User.internal.destinations
        val spinnerAdapterNotNull = spinnerAdapter ?: return
        if (userDestinationObjects.isEmpty()) return
        val userDestination = userDestinationObjects[0]

        // Create not available destination.
        val notAvailableDestination: Destination = FixedDestination()
        notAvailableDestination.description = getString(R.string.not_available)

        // Clear old list and add the not available destination.
        spinnerAdapterNotNull.clear()
        spinnerAdapterNotNull.add(notAvailableDestination)
        val activeDestination = userDestination.activeDestination
        val destinations = userDestination.destinations
        var activeIndex = 0

        // Add all possible destinations to array.
        var i = 0
        val size = destinations.size
        while (i < size) {
            val destination = destinations[i]
            spinnerAdapterNotNull.add(destination)
            if (activeDestination != null && destination.id == activeDestination.id) {
                activeIndex = i + 1
            }
            i++
        }

        // Create add destination field.
        val addDestination: Destination = FixedDestination()
        val addDestinationText = getString(R.string.fa_plus_circle) +
                "   " + getString(R.string.add_availability)
        addDestination.description = addDestinationText
        spinnerAdapterNotNull.add(addDestination)
        spinnerAdapterNotNull.notifyDataSetChanged()
        header.menu_availability_spinner.tag = activeIndex
        header.menu_availability_spinner.setSelection(activeIndex)
    }

    override fun onResponse(call: Call<Any>, response: Response<Any>) {
        if (!response.isSuccessful) {
            if (User.isLoggedIn) {
                Toast.makeText(context, getString(R.string.set_userdestination_api_fail), Toast.LENGTH_LONG).show()
            }
            val mConnectivityHelperNotNull = mConnectivityHelper ?: return
            if (!mConnectivityHelperNotNull.hasNetworkConnection()) { // First check if there is a entry already to avoid duplicates.
                if (header.menu_availability_spinner != null
                        && header.no_availability_text != null) {
                    header.menu_availability_spinner.visibility = View.GONE
                    header.no_availability_text.visibility = View.VISIBLE
                }
            }
        }
        refreshCurrentAvailability()
    }

    private class CustomFontSpinnerAdapter<D>(context: Context, resource: Int) : ArrayAdapter<Any?>(context, resource) {
        // Initialise custom font, for example:
        var font = Typeface.createFromAsset(context.assets, "fontawesome-webfont.ttf")

        // Affects default (closed) state of the spinner
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            view.typeface = font
            return view
        }

        // Affects opened state of the spinner
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view = super.getDropDownView(position, convertView, parent) as TextView
            view.typeface = font
            return view
        }
    }

    /**
     * Function to setup the availability spinner.
     */
    private fun setupSpinner() {
        val contextNotNull = context ?: return
        spinnerAdapter = CustomFontSpinnerAdapter(contextNotNull, android.R.layout.simple_spinner_item)
        val spinnerAdapterNotNull = spinnerAdapter ?: return
        spinnerAdapterNotNull.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        header.menu_availability_spinner.adapter = spinnerAdapter
        header.menu_availability_spinner.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        if (header.menu_availability_spinner.tag as Int == position || activity !is MainActivity) return
        if (!(activity as MainActivity).isConnectedToNetwork()) {
            refresh()
            Toast.makeText(context, getString(R.string.set_userdestination_api_fail), Toast.LENGTH_LONG).show()
        }
        if (parent.count - 1 == position) {
            VoIPGRIDPortalWebActivity.launchForUserDestinations(context)
        } else {
            val destination = parent.adapter.getItem(position) as Destination
            if (destination.description == getString(R.string.not_available)) {
                MiddlewareHelper.unregister(context)
            }
            val params = SelectedUserDestinationParams()
            params.fixedDestination = (destination as? FixedDestination)?.id
            params.phoneAccount = (destination as? PhoneAccount)?.id
            val call = context?.let { ServiceGenerator.createApiService(it).setSelectedUserDestination(User.internal.destinations[0].selectedUserDestination.id, params) }
            call?.enqueue(this)

            if (!MiddlewareHelper.isRegistered()) {
                // If the previous destination was not available, or if we're not registered
                // for another reason, register again.
                MiddlewareHelper.registerAtMiddleware(context)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onInternetConnectivityChanged() {
        refreshCurrentAvailability()
    }
}
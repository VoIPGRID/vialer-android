package com.voipgrid.vialer.dialer

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.ButterKnife
import butterknife.OnClick
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.calling.Dialer
import com.voipgrid.vialer.t9.T9Fragment
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.DialHelper
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.android.synthetic.main.activity_dialer.*
import kotlinx.android.synthetic.main.view_dialer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class DialerActivity : LoginRequiredActivity(), Dialer.Listener, T9Fragment.Listener {

    private val dialHelper by lazy { DialHelper.fromActivity(this) }

    private lateinit var t9Fragment: T9Fragment

    private val isContactsExpanded: Boolean
        get() = dialer.visibility != View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)
        ButterKnife.bind(this)
        VialerApplication.get().component().inject(this)
        dialer.setListener(this)
        t9Fragment = supportFragmentManager.findFragmentById(R.id.t9_search) as T9Fragment
        t9Fragment.listener = this
        preventKeyboardFromBeingDisplayed()
        handleIntents()
    }

    /**
     * Handles when this activity has been launched from outside the app, for example
     * a user clicking on a phone number link in a web browser.
     *
     */
    private fun handleIntents() {
        val action = intent.action
        val data = intent.dataString ?: return

        logger.i("Received intent to automatically launch dialer: " + intent.action + ", with data: " + data)

        val number = PhoneNumberUtils.format(data)

        when(action) {
            Intent.ACTION_CALL -> onCallNumber(number, null)
            Intent.ACTION_DIAL -> dialer.number = number
        }
    }

    /**
     * Ensures that a keyboard does not pop-up when pasting into the dialer input field
     *
     */
    private fun preventKeyboardFromBeingDisplayed() = window.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

    override fun onResume() {
        super.onResume()

        if (ConnectivityHelper.mWifiKilled) {
            connectivityHelper.useWifi(this, true)
            ConnectivityHelper.mWifiKilled = false
        }

        refreshUi()

        // If we have ever disabled the call button, we don't want the user to return
        // to this activity so just close.
        if (!button_call.isClickable) {
            finish()
        }
    }

    private fun shouldReturnNumberAsResult(): Boolean = intent.getBooleanExtra(EXTRA_RETURN_AS_RESULT, false)

    override fun numberWasChanged(phoneNumber: String) {
        t9Fragment.search(phoneNumber)
    }

    override fun digitWasPressed(digit: String) {

    }

    override fun exitButtonWasPressed() {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!allPermissionsGranted(permissions, grantResults)) {
            return
        }

        when(requestCode) {
            this.resources.getInteger(R.integer.contact_permission_request_code) -> {
                startActivity(intent)
                finish()
            }
            this.resources.getInteger(R.integer.microphone_permission_request_code) -> dialHelper.callAttemptedNumber()
        }
    }

    /**
     * Initiate an outgoing call by starting CallActivity and pass a SipUri based on the number
     *
     * @param number      number to call
     * @param contactName contact name to display
     */
    private fun onCallNumber(number: String, contactName: String?) = GlobalScope.launch(Dispatchers.Main) {
        if (isConnectedToNetwork() && !internetConnectivity.canGuaranteeAnInternetConnection()) {
            handleNoInternetAccess()
            return@launch
        }

        if (shouldReturnNumberAsResult()) {
            setResult(RESULT_DIALED_NUMBER, Intent().apply { putExtra("DIALED_NUMBER", number) })
            finish()
            return@launch
        }

        val phoneNumberToCall = PhoneNumberUtils.format(number)

        when(number.isEmpty()) {
            true -> Toast.makeText(this@DialerActivity, getString(R.string.dialer_invalid_number), Toast.LENGTH_LONG).show()
            false -> {
                dialHelper.callNumber(phoneNumberToCall, contactName)
                User.internal.lastDialledNumber = phoneNumberToCall
            }
        }
    }

    private fun handleNoInternetAccess() {
        button_call.isClickable = true

        AlertDialog.Builder(this).setTitle(R.string.dialer_no_internet_connectivity)
                .setMessage(R.string.dialer_no_internet_connectivity_subtext)
                .setCancelable(false)
                .setPositiveButton(this.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startActivityForResult(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), 1)
                }
                .show()
    }

    @OnClick(R.id.button_call)
    internal fun onCallButtonClicked() {
        when {
            dialer.number.isNotBlank() -> {
                button_call.isClickable = false
                onCallNumber(PhoneNumberUtils.format(dialer.number), null)
            }
            dialer.number.isBlank() -> dialer.number = User.internal.lastDialledNumber
        }
    }

    @OnClick(R.id.button_dialpad)
    internal fun showKeypad() {
        dialer.visibility = View.VISIBLE
        findViewById<View>(R.id.button_dialpad).visibility = View.INVISIBLE
        findViewById<View>(R.id.button_call).visibility = View.VISIBLE
        t9_search.view?.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.t9_height)
                )
    }

    private fun refreshUi() {
        button_call.setImageResource(if (shouldReturnNumberAsResult()) R.drawable.ic_call_transfer else R.drawable.ic_call_white)

        dialer.fadeIn()
        t9Fragment.show()
        no_connectivity_container.visibility = View.GONE

        if (!isConnectedToNetwork()) {
            updateUiForNoInternetConnectivity()
            return
        }
    }

    /**
     * Hides various elements to display that there is no internet connectivity.
     *
     */
    private fun updateUiForNoInternetConnectivity() {
        no_connectivity_container.visibility = View.VISIBLE
        t9Fragment.hide()
        dialer.fadeOut()
    }

    override fun onBackPressed() {
        when(isContactsExpanded) {
            true -> showKeypad()
            false -> super.onBackPressed()
        }
    }

    override fun onInternetConnectivityGained() {
        super.onInternetConnectivityGained()
        refreshUi()
    }

    override fun onInternetConnectivityLost() {
        super.onInternetConnectivityLost()
        refreshUi()
    }

    override fun onExpandRequested() {
        if (dialer.visibility != View.VISIBLE) return
        dialer.visibility = View.GONE
        t9_search.view?.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

        findViewById<View>(R.id.button_dialpad).visibility = View.VISIBLE
        findViewById<View>(R.id.button_call).visibility = View.GONE
    }

    override fun onContactSelected(number: String, name: String) {
        onCallNumber(number, name)
    }

    companion object {

        /**
         * The key for where the dialer number will be stored
         * when activity returns a result.
         *
         */
        const val RESULT_DIALED_NUMBER = 1

        /**
         * If this extra is present, the dialed number will be returned as a result
         * from the activity rather than making a call. This can be used, for example,
         * when you are trying to prompt the user to provide a transfer destination.
         *
         */
        const val EXTRA_RETURN_AS_RESULT = "EXTRA_RETURN_AS_RESULT"

        /**
         * The key in shared preferences where the last dialled number is stored.
         *
         */
        const val LAST_DIALED = "last_dialed"
    }
}
package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import butterknife.OnClick
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.calling.Dialer
import com.voipgrid.vialer.t9.T9Fragment
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.android.synthetic.main.fragment_transfer_selection.*
import kotlinx.android.synthetic.main.view_dialer.*

class TransferSelection : VoipAwareFragment(), Dialer.Listener, T9Fragment.Listener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_transfer_selection, container, false)

    private val t9Fragment = T9Fragment().apply { listener = this@TransferSelection }

    private val isContactsExpanded: Boolean
        get() = dialer.visibility != View.VISIBLE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialer.setListener(this)
        if (!t9Fragment.isAdded) {
            childFragmentManager.beginTransaction().replace(R.id.t9_search, t9Fragment).commit()
        }

        preventKeyboardFromBeingDisplayed()

        button_call.setOnClickListener {
            when {
                dialer.number.isNotBlank() -> {
                    button_call.isClickable = false
                    onCallNumber(PhoneNumberUtils.format(dialer.number))
                }
                dialer.number.isBlank() -> dialer.number = User.internal.lastDialledNumber
            }
        }
    }

    /**
     * Ensures that a keyboard does not pop-up when pasting into the dialer input field
     *
     */
    private fun preventKeyboardFromBeingDisplayed() = activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun numberWasChanged(phoneNumber: String) {
        t9Fragment.search(phoneNumber)
    }

    override fun digitWasPressed(digit: String) {

    }

    override fun exitButtonWasPressed() {

    }

    /**
     * Initiate an outgoing call by starting CallActivity and pass a SipUri based on the number
     *
     * @param number      number to call
     */
    private fun onCallNumber(number: String) {
        when(number.isEmpty()) {
            true -> Toast.makeText(activity, getString(R.string.dialer_invalid_number), Toast.LENGTH_LONG).show()
            false -> {
                voip?.initiateTransfer(PhoneNumberUtils.format(number))
                (activity as NewCallActivity).closeTransferSelector()
            }
        }
    }

    @OnClick(R.id.button_dialpad)
    internal fun showKeypad() {
        dialer.visibility = View.VISIBLE
//        findViewById<View>(R.id.button_dialpad).visibility = View.INVISIBLE
//        findViewById<View>(R.id.button_call).visibility = View.VISIBLE
    }

    private fun refreshUi() {
        dialer.fadeIn()
        t9Fragment.show()
        no_connectivity_container.visibility = View.GONE
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

    override fun onExpandRequested() {
//        if (dialer.visibility != View.VISIBLE) return
//        dialer.visibility = View.GONE
//        findViewById<View>(R.id.button_dialpad).visibility = View.VISIBLE
//        findViewById<View>(R.id.button_call).visibility = View.GONE
    }

    override fun onContactSelected(number: String, name: String) {
        onCallNumber(number)
    }
}
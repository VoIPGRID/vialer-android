package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    private val t9Fragment = T9Fragment(false).apply { listener = this@TransferSelection }

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

        button_dialpad.setOnClickListener {
            button_dialpad.visibility = View.GONE
            Log.e("TEST123", "Adjusting for container with weightsum: ${container.weightSum}")
            changeWeights(15f, 85f)
        }
    }

    /**
     * Ensures that a keyboard does not pop-up when pasting into the dialer input field
     *
     */
    private fun preventKeyboardFromBeingDisplayed() = activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

    override fun onResume() {
        super.onResume()
        dialer.fadeIn()
        t9Fragment.show()
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

    /**
     * Change the weights to adjust the amount of screen each element is taking up.
     *
     */
    private fun changeWeights(t9Weight: Float, dialerWeight: Float) {
        t9_search.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                t9Weight
        )

        dialer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                dialerWeight
        )
    }

    override fun onExpandRequested() {
        changeWeights(100f, 0f)
        button_dialpad.visibility = View.VISIBLE
    }

    override fun onContactSelected(number: String, name: String) {
        onCallNumber(number)
    }
}
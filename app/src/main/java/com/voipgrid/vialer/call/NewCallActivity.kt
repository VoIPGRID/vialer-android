package com.voipgrid.vialer.call

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.call.fragments.*
import com.voipgrid.vialer.voip.core.call.State
import com.voipgrid.vialer.voip.core.call.State.TelephonyState.*
import kotlinx.android.synthetic.main.activity_call_new.*

class NewCallActivity : NewAbstractCallActivity() {

    enum class UpperSection(val fragment: Fragment) {
        CALL_DETAILS(ActiveCallHeader()),
        INCOMING_CALL_HEADER(IncomingCallHeader()),
        TRANSFER_COMPLETE(TransferComplete())
    }

    enum class LowerSection(val fragment: Fragment) {
        CALL_ACTION_BUTTONS(ActiveCallButtons()),
        DIALER(Dialer()),
        INCOMING_CALL_BUTTONS(IncomingCallButtons()),
        TRANSFER(TransferSelection())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_call_new)

        render()
    }

    fun openDialer() {
        swapLowerSection(LowerSection.DIALER, true)
    }

    fun closeDialer() {
        swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
    }

    fun openTransferSelector() {
        voip?.getCurrentCall()?.hold()
        swapLowerSection(LowerSection.TRANSFER, true)
    }

    fun closeTransferSelector() {
        swapLowerSection(LowerSection.TRANSFER, true)
    }

    fun mergeTransfer() {
        voip?.let {
            it.mergeTransfer()
            swapUpperSection(UpperSection.TRANSFER_COMPLETE)
            render()
        }
    }

    override fun voipServiceIsAvailable() {
        super.voipServiceIsAvailable()

        voip?.getCurrentCall()?.state?.telephonyState?.let { voipStateWasUpdated(it) }
    }

    override fun voipStateWasUpdated(state: State.TelephonyState) {
        super.voipStateWasUpdated(state)

        when (state) {
            INITIALIZING -> ""
            OUTGOING_CALLING -> {
                swapUpperSection(UpperSection.CALL_DETAILS)
                swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
            }
            INCOMING_RINGING -> {
                swapLowerSection(LowerSection.INCOMING_CALL_BUTTONS)
                swapUpperSection(UpperSection.INCOMING_CALL_HEADER)
            }
            CONNECTED -> {
                swapUpperSection(UpperSection.CALL_DETAILS)
                swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
            }
            DISCONNECTED -> if (voip?.getCurrentCall() == null) {
                Log.e("TEST123", "Finishing...")
                finish()
            }
        }
    }

    override fun voipUpdate() {
        render()
    }

    override fun finish() {
        if (shouldDelayFinish()) {
            Handler().postDelayed({
                super.finish()
            }, 3000)
        } else {
            super.finish()
        }
    }

    private fun render() {
        supportFragmentManager.fragments.forEach { (it as? VoipAwareFragment)?.render() }
    }

    private fun shouldDelayFinish(): Boolean {
        return supportFragmentManager.findFragmentByTag("UPPER") is TransferComplete
    }

    private fun swapUpperSection(section: UpperSection, backstack: Boolean = false) = swapFragment(section.fragment, call_upper_section, "UPPER", backstack)

    private fun swapLowerSection(section: LowerSection, backstack: Boolean = false) = swapFragment(section.fragment, call_lower_section, "LOWER", backstack)

    private fun swapFragment(fragment: Fragment, container: View, tag: String, backstack: Boolean) {
        if (!fragment.isAdded) {
            val transaction = supportFragmentManager.beginTransaction().add(container.id, fragment, tag)
            if (backstack) transaction.addToBackStack(null)
            transaction.commit()
        }

        val current = supportFragmentManager.findFragmentByTag(when(container) {
            call_upper_section -> "UPPER"
            call_lower_section -> "LOWER"
            else -> ""
        })

        supportFragmentManager.beginTransaction()
                .also { transaction -> current?.let { transaction.hide(it) } }
                .show(fragment)
                .commit()

        (fragment as VoipAwareFragment).render()
    }

}
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
        swapLowerSection(LowerSection.DIALER)
    }

    fun closeDialer() {
        swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
    }

    fun openTransferSelector() {
        voip?.calls?.active?.hold()
        swapLowerSection(LowerSection.TRANSFER)
    }

    fun closeTransferSelector() {
        swapLowerSection(LowerSection.TRANSFER)
    }

    fun mergeTransfer() {
        voip?.let {
            it.mergeTransfer()
            swapUpperSection(UpperSection.TRANSFER_COMPLETE)
            render()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.findFragmentByTag("LOWER") !is ActiveCallButtons) {
            swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
            return
        }

        super.onBackPressed()
    }

    override fun voipServiceIsAvailable() {
        super.voipServiceIsAvailable()

        voip?.calls?.active?.state?.telephonyState?.let { voipStateWasUpdated(it) }
    }

    override fun voipStateWasUpdated(state: State.TelephonyState) {
        super.voipStateWasUpdated(state)
Log.e("TEST123", "voipStateWasUpdated ${state}")
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
            DISCONNECTED -> if (voip?.calls?.active == null) {
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

    private fun swapUpperSection(section: UpperSection) = swapFragment(section.fragment, call_upper_section, "UPPER")

    private fun swapLowerSection(section: LowerSection) = swapFragment(section.fragment, call_lower_section, "LOWER")

    private fun swapFragment(fragment: Fragment, container: View, tag: String) {
        if (!fragment.isAdded) {
            supportFragmentManager.beginTransaction().add(container.id, fragment, tag).commit()
        }

        val relevantTag = when(container) {
            call_upper_section -> "UPPER"
            call_lower_section -> "LOWER"
            else -> ""
        }

        val transaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.fragments.filter { it.tag == relevantTag }.forEach { transaction.hide(it) }

        transaction.show(fragment).commit()

        (fragment as VoipAwareFragment).render()
    }

}
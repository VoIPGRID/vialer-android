package com.voipgrid.vialer.call

import android.os.Bundle
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
        INCOMING_CALL_HEADER(IncomingCallHeader())
    }

    enum class LowerSection(val fragment: Fragment) {
        CALL_ACTION_BUTTONS(ActiveCallButtons()),
        DIALER(Dialer()),
        INCOMING_CALL_BUTTONS(IncomingCallButtons())
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
            DISCONNECTED -> finish()
        }
    }

    override fun voipUpdate() {
        render()
    }

    private fun render() {
        val voip = voip ?: return
        val call = voip.getCurrentCall() ?: return

        activeFragments().forEach { it.render() }
    }

    private fun activeFragments() : Array<VoipAwareFragment> {
        return arrayOf(supportFragmentManager.findFragmentByTag("UPPER") as VoipAwareFragment, supportFragmentManager.findFragmentByTag("LOWER") as VoipAwareFragment)
    }

    private fun swapUpperSection(section: UpperSection) = swapFragment(section.fragment, call_upper_section, "UPPER")

    private fun swapLowerSection(section: LowerSection) = swapFragment(section.fragment, call_lower_section, "LOWER")

    private fun swapFragment(fragment: Fragment, container: View, tag: String) {
        supportFragmentManager.beginTransaction().replace(container.id, fragment, tag).commit()
    }
}
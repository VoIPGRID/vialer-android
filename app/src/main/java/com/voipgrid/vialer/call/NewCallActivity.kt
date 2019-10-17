package com.voipgrid.vialer.call

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.call.fragments.ActiveCallButtons
import com.voipgrid.vialer.call.fragments.ActiveCallHeader
import com.voipgrid.vialer.calling.Dialer
import com.voipgrid.vialer.voip.core.call.State
import com.voipgrid.vialer.voip.core.call.State.TelephonyState.*
import kotlinx.android.synthetic.main.activity_call_new.*

class NewCallActivity : NewAbstractCallActivity() {

    enum class UpperSection(val fragment: Fragment) {
        CALL_DETAILS(ActiveCallHeader())
    }

    enum class LowerSection(val fragment: Fragment) {
        CALL_ACTION_BUTTONS(ActiveCallButtons()),
        DIALER(com.voipgrid.vialer.call.fragments.Dialer())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_call_new)

        render()

        swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
    }

    fun openDialer() {
        swapLowerSection(LowerSection.DIALER)
    }

    fun closeDialer() {
        swapLowerSection(LowerSection.CALL_ACTION_BUTTONS)
    }

    override fun voipServiceIsAvailable() {
        super.voipServiceIsAvailable()
    }

    override fun voipStateWasUpdated(state: State.TelephonyState) {
        super.voipStateWasUpdated(state)

        when (state) {
            INITIALIZING -> ""
            CALLING -> ""
            RINGING -> ""
            CONNECTED -> ""
            DISCONNECTED -> finish()
        }
    }

    override fun voipUpdate() {
        render()
    }

    private fun render() {
        val voip = voip ?: return
        val call = voip.getCurrentCall() ?: return

        swapUpperSection(UpperSection.CALL_DETAILS)
    }

    private fun swapUpperSection(section: UpperSection) = swapFragment(section.fragment, call_upper_section)

    private fun swapLowerSection(section: LowerSection) = swapFragment(section.fragment, call_lower_section)

    private fun swapFragment(fragment: Fragment, container: View) {
        when (fragment.isAdded) {
            true ->  supportFragmentManager.beginTransaction().replace(container.id, fragment)
            false ->  supportFragmentManager.beginTransaction().add(container.id, fragment)
        }.commit()
    }
}
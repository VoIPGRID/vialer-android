package com.voipgrid.vialer.calling

import android.os.Bundle
import com.voipgrid.vialer.R
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.core.Action
import com.voipgrid.vialer.sip.core.SipActionHandler.Companion.performActionOnSipService
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.koin.android.ext.android.inject

class IncomingCallActivity : AbstractCallActivity() {

    private val callActivityHelper: CallActivityHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        button_decline.setOnClickListener { onDeclineButtonClicked() }
        button_pickup.setOnClickListener { onPickupButtonClicked() }
    }

    /**
     * The user has pressed the decline button.
     *
     */
    fun onDeclineButtonClicked() {
        logger.d("decline")

        disableAllButtons()

        if (!sipServiceConnection.isAvailableAndHasActiveCall) return

        performActionOnSipService(this, Action.DECLINE_INCOMING_CALL)

        finish()
    }

    /**
     * The user has pressed the pick-up button.
     *
     */
    fun onPickupButtonClicked() {
        if (!sipServiceConnection.isAvailableAndHasActiveCall) {
            finish()
            return
        }
        disableAllButtons()
        performActionOnSipService(this, Action.ANSWER_INCOMING_CALL)
    }

    /**
     * Disable the on-screen buttons.
     *
     */
    private fun disableAllButtons() {
        listOf(button_pickup, button_decline).forEach { it.isEnabled = false }
    }

    override fun onDestroy() {
        super.onDestroy()
        sipServiceConnection.disconnect(true)
    }

    override fun sipServiceHasConnected(sipService: SipService) {
        super.sipServiceHasConnected(sipService)
        if (sipService.firstCall == null) finish()
        callActivityHelper.updateLabelsBasedOnPhoneNumber(incoming_caller_title, incoming_caller_subtitle, sipService.currentCall?.phoneNumber, sipService.currentCall?.callerId, profile_image)
    }

    override fun onCallConnected() = sipServiceConnection.disconnect(true)

    override fun onCallDisconnected() {
        sipServiceConnection.disconnect(true)
        finish()
    }
}
package com.voipgrid.vialer

import android.util.Log
import android.view.View
import com.voipgrid.vialer.calling.CallActivityHelper
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.phonelib.*
import com.voipgrid.vialer.sip.CallDisconnectedReason
import com.voipgrid.vialer.sip.SipConstants
import dagger.android.AndroidInjection.inject
import kotlinx.android.synthetic.main.activity_call.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.openvoipalliance.phonelib.model.CallState
import org.openvoipalliance.phonelib.model.CallState.*

/**
 * Responsible for handling all the UI elements of the CallActivity, the update
 * method of this presenter can be called at any point and it will present the call to
 * the user (labels, buttons etc.) correctly.
 *
 */
class CallPresenter internal constructor(private val mActivity: CallActivity) : KoinComponent {
    private val softPhone: SoftPhone by inject()

    private val mCallActivityHelper: CallActivityHelper = CallActivityHelper(Contacts())

    /**
     * Updates the UI based on the current state of the call.
     *
     */
    fun update() {
        if (mActivity.button_mute == null) {
            return
        }
        updateCallLabels()
        if (!softPhone.hasCall) {
            enableOrDisableCallActionButtons(false, false, false, false, false)
            hideCallDuration()
            return
        }
        val call = softPhone.call ?: return

        val state: String = call.legacyState
        updateTransferButton(state)
        when (state) {
            SipConstants.CALL_INVALID_STATE -> enableOrDisableButtons(mute = false, hold = false, dialpad = true, speaker = true, transfer = false, hangUp = true)
            SipConstants.CALL_CONNECTED_MESSAGE, SipConstants.CALL_UNHOLD_ACTION, SipConstants.CALL_PUT_ON_HOLD_ACTION -> enableOrDisableButtons(mute = true, hold = true, dialpad = true, speaker = true, transfer = true, hangUp = true)
            SipConstants.CALL_DISCONNECTED_MESSAGE -> disableAllButtons()
        }
        mActivity.button_onhold.activate(call.isOnHold())
        mActivity.button_mute.activate(softPhone.phone?.isMicrophoneMuted() ?: false)
        mActivity.button_dialpad.activate(false)
        mActivity.button_transfer.activate(false)

        if (call.isOnHold()) {
            showCallDuration()
            mActivity.duration_text_view.text = mActivity.getString(R.string.callnotification_on_hold)
        }
        else if (call.state == OutgoingRinging) {
            showCallDuration()
            mActivity.duration_text_view.text = mActivity.getString(R.string.analytics_event_label_ringing) + "..."
        }
        else if (state == SipConstants.CALL_CONNECTED_MESSAGE || state == SipConstants.CALL_UNHOLD_ACTION) {
            mActivity.duration_text_view.text = call.prettyCallDuration
            showCallDuration()
        }
        else {
            hideCallDuration()
        }

        updateAudioSourceButton()
        showDisconnectedReason(CallDisconnectedReason.fromReason(call.reason))
    }

    private fun showDisconnectedReason(reason: CallDisconnectedReason) {
        var status: String? = null
        // Should become a switch when more reasons are added
        if (reason === CallDisconnectedReason.NUMBER_NOT_FOUND) {
            status = mActivity.getString(R.string.call_disconnected_reason_not_found)
        }
        if (status != null) {
            mActivity.call_status.text = status
            mActivity.call_status.visibility = View.VISIBLE
        } else {
            mActivity.call_status.visibility = View.INVISIBLE
        }
    }

    /**
     * Update the call labels with the information about the current, relevant call.
     *
     */
    private fun updateCallLabels() {
        softPhone.call?.let {
            mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mActivity.incoming_caller_title, mActivity.incoming_caller_subtitle, it.phoneNumber, it.displayName)
        }
    }

    /**
     * Update the call transfer button to change the text/icon depending on the state of the
     * transfer.
     *
     * @param state The current call state of the primary call
     */
    private fun updateTransferButton(state: String) {
        if (softPhone.isOnTransfer) {
            mActivity.call_status.text = mActivity.getString(R.string.call_on_hold, softPhone.transferSession?.from?.phoneNumber ?: "")
            mActivity.call_status.visibility = View.VISIBLE
            mActivity.button_transfer.setImageResource(R.drawable.ic_call_merge)
            mActivity.transfer_label.setText(R.string.transfer_connect)
            if (mActivity.hasSecondCall()) {
                if (state == SipConstants.CALL_CONNECTED_MESSAGE) {
                    mActivity.button_transfer.enable()
                } else {
                    mActivity.button_transfer.disable()
                }
            }
        } else {
            mActivity.button_transfer.setImageResource(R.drawable.ic_call_transfer)
            mActivity.transfer_label.setText(R.string.transfer_label)
            mActivity.button_transfer.enable(state == SipConstants.CALL_CONNECTED_MESSAGE)
            mActivity.call_status.visibility = View.INVISIBLE
        }
    }

    /**
     * Update audio source button to the correct icon depending on what audio
     * source is currently in use.
     *
     */
    private fun updateAudioSourceButton() {
        if (mActivity.button_speaker == null) {
            return
        }
        var image = R.drawable.ic_volume_on_enabled
        var text = R.string.speaker_label
        if (mActivity.audioRouter.isBluetoothRouteAvailable) {
            if (mActivity.isOnSpeaker) {
                image = R.drawable.audio_source_dropdown_speaker
                text = R.string.speaker_label
            } else if (mActivity.audioRouter.isCurrentlyRoutingAudioViaBluetooth) {
                image = R.drawable.audio_source_dropdown_bluetooth
                text = R.string.audio_source_option_bluetooth
            } else {
                image = R.drawable.audio_source_dropdown_phone
                text = R.string.audio_source_option_phone
            }
        }
        if (mActivity.isOnSpeaker) {
            mActivity.button_speaker.activate()
        } else {
            mActivity.button_speaker.deactivate()
        }
        mActivity.speaker_label.setText(mActivity.getString(text).toLowerCase())
        mActivity.button_speaker.setImageResource(image)
    }

    /**
     * Provide a list of booleans to enable or disable the call buttons.
     *
     * @param mute
     * @param hold
     * @param dialpad
     * @param speaker
     * @param transfer
     */
    private fun enableOrDisableCallActionButtons(mute: Boolean, hold: Boolean, dialpad: Boolean, speaker: Boolean, transfer: Boolean) {
        mActivity.button_mute.enable(mute)
        mActivity.button_onhold.enable(hold)
        mActivity.button_dialpad.enable(dialpad)
        mActivity.button_speaker.enable(speaker)
        mActivity.button_transfer.enable(transfer)
    }

    /**
     * Provide a list of booleans to enable or disable all call actions as well as the hangup button.
     *
     * @param mute
     * @param hold
     * @param dialpad
     * @param speaker
     * @param transfer
     * @param hangUp
     */
    private fun enableOrDisableButtons(mute: Boolean, hold: Boolean, dialpad: Boolean, speaker: Boolean, transfer: Boolean, hangUp: Boolean) {
        enableOrDisableCallActionButtons(mute, hold, dialpad, speaker, transfer)
        if (hangUp) {
            mActivity.button_hangup.enable()
        } else {
            mActivity.button_hangup.disable()
        }
    }

    /**
     * Disables all buttons including the hangup button.
     *
     */
    private fun disableAllButtons() {
        enableOrDisableButtons(false, false, false, false, false, false)
    }

    /**
     * Show the call duration timer.
     */
    private fun showCallDuration() {
        mActivity.duration_text_view.visibility = View.VISIBLE
    }

    /**
     * Hide the call duration timer.
     *
     */
    private fun hideCallDuration() {
        mActivity.duration_text_view.visibility = View.INVISIBLE
    }

}
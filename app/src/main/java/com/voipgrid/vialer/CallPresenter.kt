package com.voipgrid.vialer

import android.util.Log
import android.view.View
import com.voipgrid.vialer.calling.CallActivityHelper
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.sip.Audio
import com.voipgrid.vialer.sip.SipCall
import com.voipgrid.vialer.sip.SipCall.TelephonyState.*
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipConstants.*
import kotlinx.android.synthetic.main.activity_call.*

/**
 * Responsible for handling all the UI elements of the CallActivity, the update
 * method of this presenter can be called at any point and it will present the call to
 * the user (labels, buttons etc.) correctly.
 *
 */
class CallPresenter internal constructor(private val mActivity: CallActivity) {

    private val mCallActivityHelper: CallActivityHelper = CallActivityHelper(Contacts())

    /**
     * Updates the UI based on the current state of the call.
     *
     */
    fun update() {
        updateCallLabels()
        if (!mActivity.sipServiceConnection.isAvailable) {
            enableOrDisableCallActionButtons(false, false, true, true, false)
            hideCallDuration()
            return
        } else if (!mActivity.sipServiceConnection.isAvailableAndHasActiveCall) {
            disableAllButtons()
            mActivity.button_hangup.disable()
            return
        }
        val call = mActivity.sipServiceConnection.get().currentCall
        val state = call!!.state.telephonyState
        updateTransferButton(state)
        when (state) {
            INITIALIZING, INCOMING_RINGING, OUTGOING_RINGING  -> enableOrDisableButtons(false, false, true, true, false, true)
            CONNECTED ->  enableOrDisableButtons(true, true, true, true, true, true)
            DISCONNECTED -> disableAllButtons()
        }
        mActivity.button_onhold.activate(call.state.isOnHold)
        mActivity.button_mute.activate(mActivity.call.state.isMuted)
        mActivity.button_dialpad.activate(false)
        mActivity.button_transfer.activate(false)
        if (state == CONNECTED) {
            showCallDuration()
        } else {
            hideCallDuration()
        }
        updateAudioSourceButton()
    }

    /**
     * Update the call labels with the information about the current, relevant call.
     *
     */
    private fun updateCallLabels() {
        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(
                mActivity.incoming_caller_title,
                mActivity.incoming_caller_subtitle,
                mActivity.currentCallDetails?.phoneNumber ?: "",
                mActivity.currentCallDetails?.displayLabel ?: "",
                mActivity.profile_image
        )
    }

    /**
     * Update the call transfer button to change the text/icon depending on the state of the
     * transfer.
     *
     * @param state The current call state of the primary call
     */
    private fun updateTransferButton(state: SipCall.TelephonyState) {
        if (mActivity.sipServiceConnection.get().isTransferring()) {
            mActivity.call_status.text = mActivity.getString(R.string.call_on_hold, mActivity.initialCallDetail?.displayLabel ?: "")
            mActivity.call_status.visibility = View.VISIBLE
            mActivity.button_transfer.setImageResource(R.drawable.ic_call_merge)
            mActivity.transfer_label.setText(R.string.transfer_connect)
            if (mActivity.hasSecondCall()) {
                if (state == CONNECTED) {
                    mActivity.button_transfer.enable()
                } else {
                    mActivity.button_transfer.disable()
                }
            }
        } else {
            mActivity.button_transfer.setImageResource(R.drawable.ic_call_transfer)
            mActivity.transfer_label.setText(R.string.transfer_label)
            mActivity.button_transfer.enable(state == CONNECTED)
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

        if (mActivity.sip.audio.isBluetoothRouteAvailable) {
            if (mActivity.isOnSpeaker) {
                image = R.drawable.audio_source_dropdown_speaker
                text = R.string.speaker_label
            } else if (mActivity.sip.audio.route == Audio.Route.BLUETOOTH) {
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
        mActivity.speaker_label.text = mActivity.getString(text).toLowerCase()
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
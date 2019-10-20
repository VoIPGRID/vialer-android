package com.voipgrid.vialer;

import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_INVALID_STATE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_PUT_ON_HOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.CALL_UNHOLD_ACTION;

import android.util.Log;
import android.view.View;

import com.voipgrid.vialer.calling.CallActivityHelper;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.sip.SipCall;

/**
 * Responsible for handling all the UI elements of the CallActivity, the update
 * method of this presenter can be called at any point and it will present the call to
 * the user (labels, buttons etc.) correctly.
 *
 */
public class CallPresenter {

    private final CallActivity mActivity;
    private CallActivityHelper mCallActivityHelper;

    CallPresenter(CallActivity activity) {
        mActivity = activity;
        mCallActivityHelper = new CallActivityHelper(new Contacts());
    }

    /**
     * Updates the UI based on the current state of the call.
     *
     */
    public void update() {
        if (mActivity.mMuteButton == null) {
            return;
        }

        updateCallLabels();

        if (!mActivity.getSipServiceConnection().isAvailable()) {
            enableOrDisableCallActionButtons(false, false, true, true, false);
            hideCallDuration();
            return;
        }
        else if (!mActivity.getSipServiceConnection().isAvailableAndHasActiveCall()) {
            disableAllButtons();
            mActivity.mHangupButton.disable();
            return;
        }

        SipCall call = mActivity.getSipServiceConnection().get().getCurrentCall();
        String state = call.getCurrentCallState();

        updateTransferButton(state);

        switch (state) {
            case CALL_INVALID_STATE:
                enableOrDisableButtons(false, false, true, true, false, true);
                break;
            case CALL_CONNECTED_MESSAGE:
            case CALL_UNHOLD_ACTION:
            case CALL_PUT_ON_HOLD_ACTION:
                enableOrDisableButtons(true, true, true, true, true, true);
                break;
            case CALL_DISCONNECTED_MESSAGE:
                disableAllButtons();
                break;
        }

        mActivity.mOnHoldButton.activate(call.isOnHold());
        mActivity.mMuteButton.activate(mActivity.isMuted());
        mActivity.mDialpadButton.activate(false);
        mActivity.mTransferButton.activate(false);

        if (state.equals(CALL_CONNECTED_MESSAGE) || state.equals(CALL_UNHOLD_ACTION)) {
            showCallDuration();
        } else {
            hideCallDuration();
        }

        updateAudioSourceButton();
    }

    /**
     * Update the call labels with the information about the current, relevant call.
     *
     */
    private void updateCallLabels() {
        if (mActivity.getForceDisplayedCallDetails() != null) {
            mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mActivity.mTitle, mActivity.mSubtitle, mActivity.getForceDisplayedCallDetails().getNumber(), mActivity.getForceDisplayedCallDetails().getCallerId(), mActivity.mContactImage);
            return;
        }

        if (mActivity.getSipServiceConnection().isAvailableAndHasActiveCall()) {
            SipCall call = mActivity.hasSecondCall() ? mActivity.getSipServiceConnection().get().getCurrentCall() : mActivity.getSipServiceConnection().get().getFirstCall();
            mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mActivity.mTitle, mActivity.mSubtitle, call.getPhoneNumber(), call.getCallerId(), mActivity.mContactImage);
        }
    }

    /**
     * Update the call initiateTransfer button to change the text/icon depending on the state of the
     * initiateTransfer.
     *
     * @param state The current call state of the primary call
     */
    private void updateTransferButton(String state) {
        if (mActivity.isOnTransfer()) {
            mActivity.mCallStatusTv.setText(mActivity.getString(R.string.call_on_hold, mActivity.getInitialCallDetail().getDisplayLabel()));
            mActivity.mCallStatusTv.setVisibility(View.VISIBLE);

//            mActivity.mTransferButton.setImageResource(R.drawable.ic_call_merge);
            mActivity.mTransferLabel.setText(R.string.transfer_connect);

            if (mActivity.hasSecondCall()) {

                if (state.equals(CALL_CONNECTED_MESSAGE)) {
                    mActivity.mTransferButton.enable();
                } else {
                    mActivity.mTransferButton.disable();
                }
            }
        } else {
//            mActivity.mTransferButton.setImageResource(R.drawable.ic_call_transfer);
            mActivity.mTransferLabel.setText(R.string.transfer_label);
            mActivity.mTransferButton.enable(state.equals(CALL_CONNECTED_MESSAGE));
            mActivity.mCallStatusTv.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Update audio source button to the correct icon depending on what audio
     * source is currently in use.
     *
     */
    private void updateAudioSourceButton() {
        if (mActivity.mSpeakerButton == null) {
            return;
        }

        int image = R.drawable.ic_volume_on_enabled;
        int text = R.string.speaker_label;

        if (mActivity.getAudioRouter().isBluetoothRouteAvailable()) {
            if (mActivity.isOnSpeaker()) {
                image = R.drawable.audio_source_dropdown_speaker;
                text = R.string.speaker_label;
            } else if (mActivity.getAudioRouter().isCurrentlyRoutingAudioViaBluetooth()) {
                image = R.drawable.audio_source_dropdown_bluetooth;
                text = R.string.audio_source_option_bluetooth;
            } else {
                image = R.drawable.audio_source_dropdown_phone;
                text = R.string.audio_source_option_phone;
            }
        }

        if(mActivity.isOnSpeaker()) {
            mActivity.mSpeakerButton.activate();
        } else {
            mActivity.mSpeakerButton.deactivate();
        }

        mActivity.mSpeakerLabel.setText(mActivity.getString(text).toLowerCase());
//        mActivity.mSpeakerButton.setImageResource(image);
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
    private void enableOrDisableCallActionButtons(boolean mute, boolean hold, boolean dialpad, boolean speaker, boolean transfer) {
        mActivity.mMuteButton.enable(mute);
        mActivity.mOnHoldButton.enable(hold);
        mActivity.mDialpadButton.enable(dialpad);
        mActivity.mSpeakerButton.enable(speaker);
        mActivity.mTransferButton.enable(transfer);
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
    private void enableOrDisableButtons(boolean mute, boolean hold, boolean dialpad, boolean speaker, boolean transfer, boolean hangUp) {
        enableOrDisableCallActionButtons(mute, hold, dialpad, speaker, transfer);
        if (hangUp) {
            mActivity.mHangupButton.enable();
        } else {
            mActivity.mHangupButton.disable();
        }
    }

    /**
     * Disables all buttons including the hangup button.
     *
     */
    private void disableAllButtons() {
        enableOrDisableButtons(false, false, false, false, false, false);
    }

    /**
     * Show the call duration timer.
     */
    private void showCallDuration() {
        mActivity.mCallDurationView.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the call duration timer.
     *
     */
    private void hideCallDuration() {
        mActivity.mCallDurationView.setVisibility(View.INVISIBLE);
    }
}

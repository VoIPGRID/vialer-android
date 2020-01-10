package com.voipgrid.vialer

import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import com.voipgrid.vialer.call.CallDetail
import com.voipgrid.vialer.call.TransferCompleteDialog
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.Dialer
import com.voipgrid.vialer.calling.NetworkAvailabilityActivity
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.sip.service.Audio
import com.voipgrid.vialer.sip.SipCall
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.service.Event
import com.voipgrid.vialer.sip.transfer.CallTransferResult
import com.voipgrid.vialer.util.NetworkUtil
import kotlinx.android.synthetic.main.activity_call.*
import org.koin.android.ext.android.inject

/**
 * CallActivity for incoming or outgoing call.
 */
class CallActivity : AbstractCallActivity(), PopupMenu.OnMenuItemClickListener, Dialer.Listener {

    private var dialog: Dialog? = null
    private val networkUtil: NetworkUtil by inject()
    private lateinit var callPresenter: CallPresenter

    /**
     * Check whether the keypad is currently being presented to the user.
     *
     * @return TRUE if the keypad is on the screen.
     */
    private val isDialpadVisible: Boolean
        get() = dialer.visibility == View.VISIBLE

    private val updateUiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnUiThread { updateUi() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        callPresenter = CallPresenter(this)
        updateUi()
        button_mute.setOnClickListener { v -> onMuteButtonClick(v) }
        button_transfer.setOnClickListener { v -> onTransferButtonClick(v) }
        button_onhold.setOnClickListener { v -> onHoldButtonClick(v) }
        button_speaker.setOnClickListener { v -> onAudioSourceButtonClick(v) }
        button_dialpad.setOnClickListener { v -> onDialpadButtonClick(v) }
        button_hangup.setOnClickListener { onDeclineButtonClicked() }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        if (!networkUtil.isOnline) {
            NetworkAvailabilityActivity.start()
        }
        broadcastReceiverManager.registerReceiverViaLocalBroadcastManager(updateUiReceiver, Event.CALL_UPDATED.name)
    }

    override fun onPause() {
        super.onPause()
        dialog?.dismiss()
        broadcastReceiverManager.unregisterReceiver(updateUiReceiver)
    }

    override fun onCallStatusChanged(status: SipCall.TelephonyState) = updateUi()

    override fun onCallDisconnected() {
        if (sipServiceConnection.isAvailableAndHasActiveCall) {
            updateUi()
        } else {
            finish()
        }
    }

    override fun onServiceStopped() {
        finish()
    }

    /**
     * Update the UI with the latest call information, this includes buttons and labels.
     *
     */
    private fun updateUi() = callPresenter.update()

    override fun onBackPressed() {
        logger.d("onBackPressed")
        if (!sipIsAlive) {
            super.onBackPressed()
            return
        }

        if (isDialpadVisible) {
            hideDialpad()
            return
        }

        hangupViaBackButton()
    }

    /**
     * Presents a confirmation box before hanging up the call.
     *
     */
    private fun hangupViaBackButton() {
        val listener = DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> hangup()
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        AlertDialog.Builder(this)
                .setMessage(R.string.call_back_button_confirmation)
                .setPositiveButton(R.string.call_back_button_confirmation_yes, listener)
                .setNegativeButton(R.string.call_back_button_confirmation_no, listener)
                .show()
    }

    private fun toggleSpeaker() {
        logger.d("toggleSpeaker")
        sip.audio.route = if (!isOnSpeaker) Audio.Route.SPEAKER else Audio.Route.WIRED_OR_EARPIECE
    }

    /**
     * Hang-up the call, this will only hang-up the current call, not any call on-hold in the background.
     *
     */
    private fun hangup() {
        if (!sipServiceConnection.isAvailable) return
        sip.actions.hangup()
    }

    private fun onMuteButtonClick(view: View?) {
        sipServiceConnection.get().currentCall?.toggleMute()
    }

    /**
     * When the user presses the transfer button we want to check if they are currently transferring a call,
     * if so this is the "merge calls" button and we should just connect the two calls.
     *
     * Otherwise we want to hold the call and let the user choose a transfer target.
     *
     */
    private fun onTransferButtonClick(view: View?) {
        if (sip.isTransferring()) {
            showCallTransferCompletedDialog(sip.actions.mergeTransfer())
            return
        }

        if (!call.state.isOnHold) {
            onHoldButtonClick(button_onhold)
        }

        startActivityForResult(Intent(this, DialerActivity::class.java).apply {
            putExtra(DialerActivity.EXTRA_RETURN_AS_RESULT, true)
        }, DialerActivity.RESULT_DIALED_NUMBER)
    }

    /**
     * When the user presses hold we just want to toggle hold for the current call.
     *
     */
    private fun onHoldButtonClick(view: View?) {
        when (call.state.isOnHold) {
            true -> sip.actions.hold()
            false -> sip.actions.unhold()
        }
    }

    /**
     * The audio source button changes depending on whether they the user has a bluetooth device connected.
     *
     * If there is no bluetooth device this is just a simple speaker toggle, if they have a bluetooth device
     * then it should list all the input methods and let them choose one.
     *
     */
    private fun onAudioSourceButtonClick(view: View?) {
        if (!sip.audio.isBluetoothRouteAvailable) {
            toggleSpeaker()
            return
        }

        val popup = PopupMenu(this, view)
        popup.menuInflater.apply {
            inflate(R.menu.menu_audio_source, popup.menu)
        }

        sip.audio.activeBluetoothDevice?.let {
            popup.menu.getItem(2).apply {
                title = "$this (${it.name})"
            }
        }

        popup.apply {
            setOnMenuItemClickListener(this@CallActivity)
            show()
        }
    }

    private fun onDeclineButtonClicked() {
        logger.i("Hangup the call")
        hangup()
    }

    private fun onDialpadButtonClick(view: View) {
        call_actions.visibility = View.GONE
        dialer.apply {
            setListener(this@CallActivity)
            visibility = View.VISIBLE
        }
    }

    override fun digitWasPressed(dtmf: String) {
        if (!sipServiceConnection.isAvailableAndHasActiveCall) {
            return
        }
        try {
            call.dialDtmf(dtmf)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.audio_source_option_phone -> sip.audio.route = Audio.Route.WIRED_OR_EARPIECE
            R.id.audio_source_option_speaker -> sip.audio.route = Audio.Route.SPEAKER
            R.id.audio_source_option_bluetooth -> sip.audio.route = Audio.Route.BLUETOOTH
        }
        updateUi()
        return false
    }

    /**
     * Hide the dialpad and make the call actions visible again.
     *
     */
    private fun hideDialpad() {
        call_actions.visibility = View.VISIBLE
        dialer.visibility = View.GONE
    }

    override fun exitButtonWasPressed() {
        hideDialpad()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null) return

        val number = data.getStringExtra("DIALED_NUMBER")

        if (number == null || number.isEmpty()) return

        sip.actions.startTransfer(number)
    }

    override fun numberWasChanged(number: String) {}

    override fun sipServiceHasConnected(sipService: SipService) {
        super.sipServiceHasConnected(sipService)
        updateUi()
    }

    /**
     * Determine if there are currently two calls, this would suggest a transfer
     * is in progress.
     *
     * @return TRUE if there is a second call, otherwise FALSE
     */
    fun hasSecondCall(): Boolean {
        return sipServiceConnection.isAvailableAndHasActiveCall && sip.isTransferring()
    }
    
    /**
     * Display an alert informing the user that the call merge has occurred successfully.
     *
     */
    private fun showCallTransferCompletedDialog(result: CallTransferResult) {
        if (isFinishing) return

        dialog = TransferCompleteDialog.createAndShow(this, result.initialNumber, result.targetNumber)

        Handler().postDelayed({
            dialog = null
            finish()
        }, 3000)
    }

    val isOnSpeaker: Boolean
        get() = sip.audio.route == Audio.Route.SPEAKER

    override fun finish() {
        if (dialog == null) super.finish()
    }

    val initialCallDetail: CallDetail?
        get() = CallDetail.fromSipCall(sipServiceConnection.get()?.firstCall)

    val currentCallDetails: CallDetail?
        get() = CallDetail.fromSipCall(sipServiceConnection.get()?.currentCall)
}
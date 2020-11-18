package com.voipgrid.vialer

import android.app.AlertDialog
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import butterknife.ButterKnife
import butterknife.OnClick
import cn.pedant.SweetAlert.SweetAlertDialog
import com.voipgrid.vialer.call.TransferCompleteDialog
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.Dialer
import com.voipgrid.vialer.calling.NetworkAvailabilityActivity
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.phonelib.isOnHold
import com.voipgrid.vialer.sip.CallDisconnectedReason
import com.voipgrid.vialer.util.NetworkUtil
import kotlinx.android.synthetic.main.activity_call.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openvoipalliance.phonelib.model.Reason
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * CallActivity for incoming or outgoing call.
 */
class CallActivity : AbstractCallActivity(), PopupMenu.OnMenuItemClickListener, Dialer.Listener, KoinComponent {

    val mNetworkUtil: NetworkUtil by inject()
    private val callPresenter by lazy { CallPresenter(this) }
    private var mTransferCompleteDialog: SweetAlertDialog? = null
    private val updateUiReceiver = UpdateUiReceiver()
    private var uiTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        updateUi()
        ButterKnife.bind(this)
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        if (!mNetworkUtil.isOnline) {
            NetworkAvailabilityActivity.start()
        }
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(updateUiReceiver, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED, Intent.ACTION_HEADSET_PLUG)
        uiTimer = fixedRateTimer("ui", false, 0, 1000) {
            runOnUiThread { updateUi() }
        }
    }

    override fun onPause() {
        super.onPause()
        mTransferCompleteDialog?.dismiss()
        broadcastReceiverManager.unregisterReceiver(updateUiReceiver)
        uiTimer?.cancel()
    }

    override fun onCallStatusChanged(status: String, callId: String) {
        updateUi()
    }

    override fun onCallConnected() {
        updateUi()
    }

    override fun onCallDisconnected(reason: String?) {
        if (!softPhone.hasCall) {
            finish()
        }
    }

    /**
     * Update the UI with the latest call information, this includes buttons and labels.
     *
     */
    private fun updateUi() {
        if (!softPhone.hasCall && mTransferCompleteDialog == null) {
            finish()
            return
        }
        callPresenter.update()
    }

    override fun onBackPressed() {
        logger.d("onBackPressed")

        if (!softPhone.hasCall) {
            super.onBackPressed()
            return
        }

        if (softPhone.isOnTransfer || button_hangup.visibility == View.VISIBLE) {
            hangupViaBackButton()
        } else if (isDialpadVisible) {
            hideDialpad()
        }
    }

    /**
     * Presents a confirmation box before hanging up the call.
     *
     */
    private fun hangupViaBackButton() {
        val listener = DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> hangup()
                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }
        AlertDialog.Builder(this)
                .setMessage(R.string.call_back_button_confirmation)
                .setPositiveButton(R.string.call_back_button_confirmation_yes, listener)
                .setNegativeButton(R.string.call_back_button_confirmation_no, listener)
                .show()
    }

    // Toggle the call on speaker when the user presses the button.
    private fun toggleSpeaker() {
        logger.d("toggleSpeaker")
        if (!audioRouter.isCurrentlyRoutingAudioViaSpeaker) {
            audioRouter.routeAudioViaSpeaker()
        } else {
            audioRouter.routeAudioViaEarpiece()
        }
        updateUi()
    }

    /**
     * Hang-up the call, this will only hang-up the current call, not any call on-hold in the background.
     *
     */
    fun hangup() {
        softPhone.call?.let { softPhone.phone?.actions(it)?.end() }
        updateUi()
    }

    @OnClick(R.id.button_mute)
    fun onMuteButtonClick(view: View?) {
        softPhone.phone?.microphoneMuted = !(softPhone.phone?.microphoneMuted ?: false)
        updateUi()
    }

    @OnClick(R.id.button_transfer)
    fun onTransferButtonClick(view: View?) {
        if (softPhone.isOnTransfer) {
            softPhone.finishAttendedTransfer()
            showCallTransferCompletedDialog()
            return
        }

        val intent = Intent(this, DialerActivity::class.java)
        intent.putExtra(DialerActivity.EXTRA_RETURN_AS_RESULT, true)
        startActivityForResult(intent, DialerActivity.RESULT_DIALED_NUMBER)
    }

    @OnClick(R.id.button_onhold)
    fun onHoldButtonClick(view: View?) {
        softPhone.call?.let { softPhone.phone?.actions(it)?.hold(!it.isOnHold()) }
        updateUi()
    }

    @OnClick(R.id.button_speaker)
    fun onAudioSourceButtonClick(view: View?) {
        if (!audioRouter.isBluetoothRouteAvailable) {
            toggleSpeaker()
            return
        }
        val bluetoothDevice = audioRouter.connectedBluetoothHeadset
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_audio_source, popup.menu)
        if (bluetoothDevice != null) {
            val menuItem = popup.menu.getItem(2)
            menuItem.title = menuItem.toString() + " (" + bluetoothDevice.name + ")"
        }
        popup.setOnMenuItemClickListener(this)
        popup.show()
        updateUi()
    }

    @OnClick(R.id.button_hangup)
    public override fun onDeclineButtonClicked() {
        logger.i("Hangup the call")
        hangup()
        updateUi()
    }

    @OnClick(R.id.button_dialpad)
    fun onDialpadButtonClick(view: View?) {
        call_actions.visibility = View.GONE
        dialer.setListener(this)
        dialer.visibility = View.VISIBLE
    }

    override fun digitWasPressed(dtmf: String) {
        softPhone.call?.let {
            softPhone.phone?.actions(it)?.sendDtmf(dtmf)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.audio_source_option_phone -> audioRouter.routeAudioViaEarpiece()
            R.id.audio_source_option_speaker -> audioRouter.routeAudioViaSpeaker()
            R.id.audio_source_option_bluetooth -> audioRouter.routeAudioViaBluetooth()
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
        if (data == null) {
            return
        }
        val number = data.getStringExtra("DIALED_NUMBER")
        if (number == null || number.isEmpty()) {
            return
        }

        try {
            softPhone.beginAttendedTransfer(number)
        } catch (e: SecurityException) {
            logger.e("Unable to begin call transfer, permission issue.")
        }
    }


    override fun numberWasChanged(number: String) {}

    /**
     * Check whether the keypad is currently being presented to the user.
     *
     * @return TRUE if the keypad is on the screen.
     */
    private val isDialpadVisible: Boolean
        private get() = dialer.visibility == View.VISIBLE


    /**
     * Determine if there are currently two calls, this would suggest a transfer
     * is in progress.
     *
     * @return TRUE if there is a second call, otherwise FALSE
     */
    fun hasSecondCall() = softPhone.isOnTransfer

    /**
     * Display an alert informing the user that the call merge has occurred successfully.
     *
     */
    private fun showCallTransferCompletedDialog() {
        if (isFinishing) {
            return
        }

        softPhone.transferCall?.let {
            mTransferCompleteDialog = TransferCompleteDialog.createAndShow(this, it.from.phoneNumber, it.to.phoneNumber)
            finishAfterTransferDialogIsComplete()
        }
    }

    val isOnSpeaker: Boolean
        get() = audioRouter.isCurrentlyRoutingAudioViaSpeaker

    override fun finish() {
        if (mTransferCompleteDialog != null && mTransferCompleteDialog!!.isShowing) {
            finishAfterTransferDialogIsComplete()
            return
        }
        super.finish()
    }

    private fun finishAfterTransferDialogIsComplete() {
        Handler().postDelayed({
            mTransferCompleteDialog?.cancel()
            mTransferCompleteDialog = null
            super.finish()
        }, 3000)
    }
    /**
     * Updates the UI whenever any events are received.
     *
     */
    private inner class UpdateUiReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runOnUiThread { updateUi() }
        }
    }
}
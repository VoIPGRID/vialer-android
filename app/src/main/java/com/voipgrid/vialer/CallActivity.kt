package com.voipgrid.vialer

import android.app.AlertDialog
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import butterknife.ButterKnife
import butterknife.OnClick
import cn.pedant.SweetAlert.SweetAlertDialog
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.call.CallDetail
import com.voipgrid.vialer.call.DisplayCallDetail
import com.voipgrid.vialer.call.TransferCompleteDialog
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.CallingConstants
import com.voipgrid.vialer.calling.Dialer
import com.voipgrid.vialer.calling.NetworkAvailabilityActivity
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.phonelib.callId
import com.voipgrid.vialer.phonelib.isConnected
import com.voipgrid.vialer.phonelib.isOnHold
import com.voipgrid.vialer.sip.CallDisconnectedReason
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.SipUri
import com.voipgrid.vialer.statistics.VialerStatistics
import com.voipgrid.vialer.util.NetworkUtil
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.android.synthetic.main.activity_call.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.openvoipalliance.phonelib.PhoneLib
import javax.inject.Inject

/**
 * CallActivity for incoming or outgoing call.
 */
class CallActivity : AbstractCallActivity(), PopupMenu.OnMenuItemClickListener, Dialer.Listener, KoinComponent {

    private val phoneLib: PhoneLib by inject()

    @JvmField @Inject var mNetworkUtil: NetworkUtil? = null


    private var mCallPresenter: CallPresenter? = null
    private var mTransferCompleteDialog: SweetAlertDialog? = null
    private val updateUiReceiver = UpdateUiReceiver()
    private var mConnected = false
    var isOnTransfer = false
        private set
    private var mType: String? = null
    private var mCallIsTransferred = false
    var initialCallDetail: CallDetail? = null
        private set
    var transferCallDetail: CallDetail? = null
        private set

    /**
     * The call details stored in this property will always be displayed
     * on the screen, this is only used before the appropriate call has been
     * setup correctly, when set to null the information will be pulled from
     * the SipCall object directly.
     *
     */
    var forceDisplayedCallDetails: DisplayCallDetail? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        ButterKnife.bind(this)
        get().component().inject(this)
        mCallPresenter = CallPresenter(this)
        updateUi()
        mType = intent.type
        mConnected = intent.getBooleanExtra(CallingConstants.CALL_IS_CONNECTED, false)
        if (CallingConstants.TYPE_INCOMING_CALL != mType && CallingConstants.TYPE_OUTGOING_CALL != mType) {
            return
        }
        forceDisplayedCallDetails = DisplayCallDetail(intent.getStringExtra(CallingConstants.PHONE_NUMBER), intent.getStringExtra(CallingConstants.CONTACT_NAME))
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        if (!mNetworkUtil!!.isOnline) {
            NetworkAvailabilityActivity.start()
        }
        broadcastReceiverManager!!.registerReceiverViaGlobalBroadcastManager(updateUiReceiver, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED, Intent.ACTION_HEADSET_PLUG)
    }

    override fun onPause() {
        super.onPause()
        if (mTransferCompleteDialog != null) {
            mTransferCompleteDialog!!.dismiss()
        }
        broadcastReceiverManager!!.unregisterReceiver(updateUiReceiver)
    }

    override fun onCallStatusChanged(status: String, callId: String) {
        updateUi()
    }

    override fun onCallConnected() {
        mConnected = true
        forceDisplayedCallDetails = null
        if (softPhone.hasCall) {
            initialCallDetail = CallDetail.fromSipCall(softPhone.call)
        }

        if (softPhone.isOnTransfer) {
            softPhone.transferSession?.to?.let {
                transferCallDetail = CallDetail.fromSipCall(it)
            }
        }

        if (sipServiceConnection!!.isAvailableAndHasActiveCall) {
            VialerStatistics.callWasSuccessfullySetup(sipServiceConnection!!.get().currentCall)
        }
        updateUi()
    }

    override fun onCallDisconnected(reason: CallDisconnectedReason) {
        isOnTransfer = false
        if (mCallIsTransferred) {
            showCallTransferCompletedDialog()
        }
        if (sipServiceConnection!!.isAvailableAndHasActiveCall) {
            forceDisplayedCallDetails = null
            mConnected = true
            updateUi()
        } else {
            mCallPresenter!!.showDisconnectedReason(reason)
            mConnected = false
            finish()
        }
    }

    /**
     * Update the UI with the latest call information, this includes buttons and labels.
     *
     */
    private fun updateUi() {
        if (mCallPresenter == null) {
            return
        }
        mCallPresenter!!.update()
    }

    override fun onBackPressed() {
        logger.d("onBackPressed")
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall) {
            super.onBackPressed()
            return
        }
        if (isOnTransfer) {
            if (softPhone.hasCall) {
                super.onBackPressed()
            } else {
                hangupViaBackButton()
            }
        } else if (button_hangup != null && button_hangup!!.visibility == View.VISIBLE && sipServiceConnection!!.get().currentCall != null) {
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

    // Toggle the hold the call when the user presses the button.
    private fun toggleOnHold() {
        logger.d("toggleOnHold")
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall) {
            return
        }
        try {
            softPhone.call?.let {
                phoneLib.setHold(it, it.isOnHold())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Hang-up the call, this will only hang-up the current call, not any call on-hold in the background.
     *
     */
    fun hangup() {
        if (!sipServiceConnection!!.isAvailable) {
            return
        }
        if (isOnTransfer) {
            callTransferHangupSecondCall()
            updateUi()
            return
        }
        try {
            phoneLib.end(sipServiceConnection!!.get().currentCall)
            updateUi()
        } catch (ignored: Exception) {
        } finally {
            finish()
        }
    }

    @OnClick(R.id.button_mute)
    fun onMuteButtonClick(view: View?) {
        if (isOnTransfer && sipServiceConnection!!.get().currentCall.isConnected() || mConnected) {
            if (sipServiceConnection!!.isAvailableAndHasActiveCall) {
                phoneLib.setMicrophone(!phoneLib.isMicrophoneMuted())
            }
        }
    }

    @OnClick(R.id.button_transfer)
    fun onTransferButtonClick(view: View?) {
        if (!mConnected) {
            return
        }
        if (isOnTransfer) {
            callTransferConnectTheCalls()
            return
        }
        if (!isCallOnHold) {
            onHoldButtonClick(null)
        }
        val intent = Intent(this, DialerActivity::class.java)
        intent.putExtra(DialerActivity.EXTRA_RETURN_AS_RESULT, true)
        startActivityForResult(intent, DialerActivity.RESULT_DIALED_NUMBER)
    }

    @OnClick(R.id.button_onhold)
    fun onHoldButtonClick(view: View?) {
        if (isOnTransfer && sipServiceConnection!!.get().currentCall.isConnected() || mConnected) {
            if (sipServiceConnection!!.isAvailableAndHasActiveCall) {
                toggleOnHold()
            }
        }
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
    }

    @OnClick(R.id.button_hangup)
    public override fun onDeclineButtonClicked() {
        logger.i("Hangup the call")
        hangup()
    }

    @OnClick(R.id.button_dialpad)
    fun onDialpadButtonClick(view: View?) {
        call_actions.visibility = View.GONE
        dialer.setListener(this)
        dialer.visibility = View.VISIBLE
    }

    override fun digitWasPressed(dtmf: String) {
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall) {
            return
        }
        try {
            //sipServiceConnection!!.get().currentCall.dialDtmf(dtmf) @TODO
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun callTransferMakeSecondCall(numberToCall: String?) {
        val sipAddressUri = SipUri.sipAddressUri(
                applicationContext,
                PhoneNumberUtils.format(numberToCall)
        )
        sipServiceConnection!!.get().makeCall(numberToCall)
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
            isOnTransfer = false
            return
        }
        val number = data.getStringExtra("DIALED_NUMBER")
        if (number == null || number.isEmpty()) {
            return
        }
        beginCallTransferTo(number)
    }

    private fun beginCallTransferTo(number: String) {
        isOnTransfer = true
        callTransferMakeSecondCall(number)
        forceDisplayedCallDetails = DisplayCallDetail(number, null)
        updateUi()
    }

    override fun numberWasChanged(number: String) {}

    private fun callTransferHangupSecondCall() {
        forceDisplayedCallDetails = null
        try {
            softPhone.transferSession?.let {
                phoneLib.end(it.to)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * This method will connect the two calls that we have setup and send the appropriate analytics events.
     *
     */
    private fun callTransferConnectTheCalls() {
        try {
            softPhone.transferSession?.let {
                phoneLib.finishAttendedTransfer(it)
            }

            mCallIsTransferred = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check whether the keypad is currently being presented to the user.
     *
     * @return TRUE if the keypad is on the screen.
     */
    private val isDialpadVisible: Boolean
        private get() = dialer.visibility == View.VISIBLE

    /**
     * Returns TRUE if the call that started this activity is an incoming
     * call.
     *
     * @return TRUE if incoming
     */
    private val isIncomingCall: Boolean
        private get() = CallingConstants.TYPE_INCOMING_CALL == mType

    override fun sipServiceHasConnected(sipService: SipService) {
        super.sipServiceHasConnected(sipService)
        if (isIncomingCall) {
            onCallConnected()
        }
        updateUi()
    }

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
        if (isFinishing || initialCallDetail == null) {
            return
        }
        if (transferCallDetail != null && mTransferCompleteDialog == null) {
            mTransferCompleteDialog = TransferCompleteDialog.createAndShow(this, initialCallDetail!!.phoneNumber, transferCallDetail!!.phoneNumber)
        }
    }

    val isMuted = phoneLib.isMicrophoneMuted()

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
            if (mTransferCompleteDialog != null) {
                mTransferCompleteDialog!!.cancel()
                mTransferCompleteDialog = null
            }
            finish()
        }, 3000)
    }

    /**
     * Check if the primary call is on hold.
     *
     * @return TRUE if it is on hold, otherwise FALSE.
     */
    val isCallOnHold: Boolean
        get() = if (sipServiceConnection!!.isAvailableAndHasActiveCall) {
            sipServiceConnection!!.get().currentCall.isOnHold()
        } else false

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
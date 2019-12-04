package com.voipgrid.vialer.calling

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.WindowManager
import androidx.annotation.CallSuper
import butterknife.Optional
import com.voipgrid.vialer.CallActivity
import com.voipgrid.vialer.calling.SipServiceConnection.SipServiceConnectionListener
import com.voipgrid.vialer.permissions.MicrophonePermission
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.util.ProximitySensorHelper
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.koin.android.ext.android.inject

abstract class AbstractCallActivity : LoginRequiredActivity(), SipServiceConnectionListener, CallDurationTracker.Listener, CallStatusReceiver.Listener {

    val sipServiceConnection = SipServiceConnection(this)
    protected val callDurationTracker = CallDurationTracker(sipServiceConnection)
    protected val callStatusReceiver = CallStatusReceiver(this)
    private lateinit var proximitySensorHelper: ProximitySensorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        proximitySensorHelper = ProximitySensorHelper(this)
        requestMicrophonePermissionIfNecessary()
        configureActivityFlags()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onStart() {
        super.onStart()
        broadcastReceiverManager.registerReceiverViaLocalBroadcastManager(callStatusReceiver, SipConstants.ACTION_BROADCAST_CALL_STATUS)
    }

    override fun onResume() {
        super.onResume()
        sipServiceConnection.connect()
        callDurationTracker.start(this)
        proximitySensorHelper.startSensor(screen_off)
    }

    override fun onPause() {
        super.onPause()
        sipServiceConnection.disconnect()
        proximitySensorHelper.stopSensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcastReceiverManager.unregisterReceiver(callStatusReceiver)
    }

    @CallSuper
    override fun sipServiceHasConnected(sipService: SipService) {
        if (sipService.firstCall == null) {
            finish()
        }
    }

    @CallSuper
    override fun sipServiceBindingFailed() {
    }

    @CallSuper
    override fun sipServiceHasBeenDisconnected() {
    }

    private fun requestMicrophonePermissionIfNecessary() {
        if (!MicrophonePermission.hasPermission(this)) {
            MicrophonePermission.askForPermission(this)
        }
    }

    private fun configureActivityFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    @Optional
    override fun onCallDurationUpdate(seconds: Long) {
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall || !sipServiceConnection!!.get().currentCall!!.isConnected || duration_text_view == null) {
            return
        }

        duration_text_view.text = DateUtils.formatElapsedTime(seconds)
    }

    companion object {
        fun createIntentForCallActivity(context: Context): Intent {
            val intent = Intent(context, CallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }

        fun createIntentForIncomingCallActivity(context: Context?): Intent {
            val intent = Intent(context, IncomingCallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}
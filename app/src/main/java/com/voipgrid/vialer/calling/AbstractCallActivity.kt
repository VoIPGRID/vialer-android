package com.voipgrid.vialer.calling

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.PowerManager
import android.text.format.DateUtils
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.CallSuper
import butterknife.BindView
import butterknife.Optional
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.calling.SipServiceConnection.SipServiceConnectionListener
import com.voipgrid.vialer.permissions.MicrophonePermission
import com.voipgrid.vialer.phonelib.SoftPhone
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.LoginRequiredActivity
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.openvoipalliance.phonelib.PhoneLib
import javax.inject.Inject

abstract class AbstractCallActivity : LoginRequiredActivity(), SipServiceConnectionListener, CallDurationTracker.Listener, CallStatusReceiver.Listener, KoinComponent {

    val softPhone: SoftPhone by inject()

    var sipServiceConnection: SipServiceConnection? = null
    private var mCallDurationTracker: CallDurationTracker? = null
    private var mCallStatusReceiver: CallStatusReceiver? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @JvmField @BindView(R.id.duration_text_view) var mCallDurationView: TextView? = null

    @JvmField @Inject var mBroadcastReceiverManager: BroadcastReceiverManager? = null

    protected val phoneNumberFromIntent: String
        get() = softPhone.call?.phoneNumber ?: ""
    protected val callerIdFromIntent: String?
        get() = softPhone.call?.displayName ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        get().component().inject(this)
        sipServiceConnection = SipServiceConnection(this)
        mCallDurationTracker = CallDurationTracker(sipServiceConnection)
        mCallStatusReceiver = CallStatusReceiver(this)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager!!.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager!!.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "vialer:in-call")
            wakeLock?.acquire(10*60*1000L)
        }
        requestMicrophonePermissionIfNecessary()
        configureActivityFlags()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onStart() {
        super.onStart()
        mBroadcastReceiverManager!!.registerReceiverViaLocalBroadcastManager(mCallStatusReceiver, SipConstants.ACTION_BROADCAST_CALL_STATUS)
    }

    override fun onResume() {
        super.onResume()
        sipServiceConnection!!.connect()
        mCallDurationTracker!!.start(this)
    }

    override fun onPause() {
        super.onPause()
        sipServiceConnection!!.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBroadcastReceiverManager!!.unregisterReceiver(mCallStatusReceiver)
        if (wakeLock != null) {
            wakeLock!!.release()
        }
    }

    @CallSuper
    override fun sipServiceHasConnected(sipService: SipService) {
        if (sipService.currentCall == null) {
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

    protected open fun onPickupButtonClicked() {}
    protected open fun onDeclineButtonClicked() {}

    @Optional
    override fun onCallDurationUpdate(seconds: Long) {
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall || mCallDurationView == null) {
            return
        }
        mCallDurationView!!.text = DateUtils.formatElapsedTime(seconds)
    }

    private val callerInfo: String?
        private get() = if (callerIdFromIntent != null && !callerIdFromIntent!!.isEmpty()) {
            callerIdFromIntent
        } else phoneNumberFromIntent
    val audioRouter: AudioRouter
        get() = sipServiceConnection!!.get().audioRouter

    companion object {
        @JvmStatic
        fun createIntentForCallActivity(caller: Context?, activity: Class<*>?): Intent {
            val intent = Intent(caller, activity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}
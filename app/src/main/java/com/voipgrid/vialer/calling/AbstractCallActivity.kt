package com.voipgrid.vialer.calling

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.permissions.MicrophonePermission
import com.voipgrid.vialer.phonelib.SoftPhone
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.util.LoginRequiredActivity
import org.koin.core.KoinComponent
import org.koin.core.inject

abstract class AbstractCallActivity : LoginRequiredActivity(), CallStatusReceiver.Listener, KoinComponent {

    val softPhone: SoftPhone by inject()
    val audioRouter: AudioRouter by inject()

    private var mCallStatusReceiver: CallStatusReceiver? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    protected val phoneNumberFromIntent: String
        get() = softPhone.call?.phoneNumber ?: ""
    protected val callerIdFromIntent: String?
        get() = softPhone.call?.displayName ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        get().component().inject(this)
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
        broadcastReceiverManager.registerReceiverViaLocalBroadcastManager(mCallStatusReceiver, SipConstants.ACTION_BROADCAST_CALL_STATUS)
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcastReceiverManager.unregisterReceiver(mCallStatusReceiver)
        wakeLock?.release()
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

    companion object {
        @JvmStatic
        fun createIntentForCallActivity(caller: Context?, activity: Class<*>?): Intent {
            val intent = Intent(caller, activity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}
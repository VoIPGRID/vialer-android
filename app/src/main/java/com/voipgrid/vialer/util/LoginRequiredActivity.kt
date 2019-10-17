package com.voipgrid.vialer.util

import android.content.*
import android.os.Bundle
import android.os.IBinder
import com.voipgrid.vialer.User
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.VialerBaseActivity
import com.voipgrid.vialer.onboarding.OnboardingActivity
import com.voipgrid.vialer.voip.VoipService
import com.voipgrid.vialer.voip.core.call.State

abstract class LoginRequiredActivity : VialerBaseActivity() {

    private var receiver: BroadcastReceiver? = null
    var voip: VoipService? = null

    override val logger = Logger(this)

    private var bound = false
    private val connection = VoipServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Intent(this, VoipService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!User.isLoggedIn) {
            logger.w("Not logged in anymore! Redirecting to onboarding")
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        receiver = broadcastReceiverManager.registerReceiverViaLocalBroadcastManager(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                
                when (intent.action) {
                    VoipService.ACTION_CALL_STATE_WAS_UPDATED -> voipStateWasUpdated(intent.getSerializableExtra(VoipService.CALL_STATE_EXTRA) as State.TelephonyState)
                    VoipService.ACTION_VOIP_UPDATE -> voipUpdate()
                }
            }

        }, VoipService.ACTION_CALL_STATE_WAS_UPDATED, VoipService.ACTION_VOIP_UPDATE)
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiverManager.unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    protected open fun voipServiceIsAvailable() {}

    protected open fun voipStateWasUpdated(state: State.TelephonyState) {}

    protected open fun voipUpdate() {}

    inner class VoipServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as VoipService.LocalBinder
            voip = binder.getService()
            voipServiceIsAvailable()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
        }
    }
}
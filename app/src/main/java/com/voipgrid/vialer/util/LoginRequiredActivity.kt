package com.voipgrid.vialer.util

import android.content.*
import android.os.Bundle
import android.os.IBinder
import com.voipgrid.vialer.User
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.VialerBaseActivity
import com.voipgrid.vialer.onboarding.OnboardingActivity
import nl.voipgrid.vialer_voip.VoipService
import nl.voipgrid.vialer_voip.android.BindableService
import nl.voipgrid.vialer_voip.core.call.State

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
                    VoipService.Events.CALL_STATE_HAS_CHANGED.name -> voipStateWasUpdated(intent.getSerializableExtra(VoipService.Extras.CALL_STATE.name) as State.TelephonyState)
                    VoipService.Events.VOIP_TIC.name -> voipUpdate()
                }
            }

        }, VoipService.Events.CALL_STATE_HAS_CHANGED.name, VoipService.Events.VOIP_TIC.name)
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

        @Suppress("UNCHECKED_CAST")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as BindableService<VoipService>.LocalBinder
            voip = binder.getService()
            voipServiceIsAvailable()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
        }
    }
}
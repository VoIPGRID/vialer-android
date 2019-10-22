package com.voipgrid.vialer.tasks.launch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.voip.VoipService
import com.voipgrid.vialer.voip.VoipService.Events.*
import com.voipgrid.vialer.voip.android.VoipEventReceiver
import com.voipgrid.vialer.voip.core.Configuration
import com.voipgrid.vialer.voip.core.Credentials
import com.voipgrid.vialer.voip.core.SipHost
import com.voipgrid.vialer.voip.core.call.Call
import com.voipgrid.vialer.voip.core.call.State
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject

class ConfigureVoip : OnLaunchTask, KoinComponent {

    override fun execute(application: VialerApplication) {
        get<BroadcastReceiverManager>().registerReceiverViaLocalBroadcastManager(globalVoipEventHandler, OUTGOING_CALL_HAS_BEEN_SETUP.name, INCOMING_CALL_IS_RINGING.name, CALL_STATE_HAS_CHANGED.name)

        VoipService.credentials = {
            Credentials(
                    accountId = User.voipAccount?.accountId ?: "",
                    password = User.voipAccount?.password ?: ""
            )
        }

        VoipService.configuration = {
            Configuration(
                    host = SipHost(application.getString(if (User.voip.hasTlsEnabled) R.string.sip_host_secure else R.string.sip_host)),
                    scheme = application.getString(R.string.sip_auth_scheme),
                    realm = application.getString(R.string.sip_auth_realm),
                    transport = if (User.voip.hasTlsEnabled) Configuration.Transport.TLS else Configuration.Transport.TCP,
                    stun = User.voip.hasStunEnabled,
                    userAgent = "vialer-test-ua",
                    codec = Configuration.Codec.OPUS
            )
        }
    }

    companion object {
        private val globalVoipEventHandler: GlobalVoipEventHandler by lazy { GlobalVoipEventHandler() }
    }

    private class GlobalVoipEventHandler : VoipEventReceiver(), KoinComponent {

        private val incomingCallAlerts: IncomingCallAlerts by inject()
        private val context: Context by inject()

        override fun incomingCallHasStartedRinging() {
            incomingCallAlerts.start()
            openActivity()
        }

        override fun incomingCallHasStoppedRinging() {
            incomingCallAlerts.stop()
        }

        override fun outgoingCallHasBeenSetup() {
            openActivity()
        }

        /**
         * Launch the call activity.
         *
         */
        private fun openActivity() {
            context.startActivity(Intent(context, NewCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

    }
}
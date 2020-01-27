package com.voipgrid.vialer.sip

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import android.telecom.CallAudioState
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.android.calling.AndroidCallConnection
import com.voipgrid.vialer.android.calling.AndroidCallManager
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.logging.LogHelper
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.sip.SipLogHandler
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.sip.SipCall.TelephonyState.*
import com.voipgrid.vialer.sip.core.Action
import com.voipgrid.vialer.sip.core.CallListener
import com.voipgrid.vialer.sip.core.CallStack
import com.voipgrid.vialer.sip.core.SipActionHandler
import com.voipgrid.vialer.sip.incoming.MiddlewareResponse
import com.voipgrid.vialer.sip.outgoing.OutgoingCallRinger

import com.voipgrid.vialer.sip.pjsip.Pjsip
import com.voipgrid.vialer.sip.pjsip.Pjsip.SipAccount
import com.voipgrid.vialer.sip.service.*
import com.voipgrid.vialer.sip.service.SipServiceNotificationManager.Type.*
import com.voipgrid.vialer.sip.utils.BusyTone
import com.voipgrid.vialer.sip.utils.ScreenOffReceiver
import com.voipgrid.vialer.util.BroadcastReceiverManager
import org.koin.android.ext.android.inject
import org.pjsip.pjsua2.OnIncomingCallParam

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
class SipService : Service() {

    private val calls = CallStack()

    val hasCall: Boolean get() = calls.isNotEmpty()
    val currentCall: SipCall? get() = calls.current
    val firstCall: SipCall? get() = calls.initial

    private val mBinder: IBinder = SipServiceBinder()

    private val sipActionHandler = SipActionHandler(this)
    private val notification = SipServiceNotificationManager(this)

    private val logger = Logger(this)
    private val broadcastReceiverManager: BroadcastReceiverManager by inject()
    private val networkConnectivity: NetworkConnectivity by inject()
    private val androidCallManager: AndroidCallManager by inject()
    private val ipSwitchMonitor: IpSwitchMonitor by inject()
    private val monitor: SipServiceMonitor by inject()
    private val screenOffReceiver: ScreenOffReceiver by inject()
    private val localBroadcastManager: LocalBroadcastManager by inject()
    private val pjsip: Pjsip by inject()

    val audio by lazy { Audio(connection) }
    val actions by lazy { Actions(this, connection, androidCallManager, pjsip) }
    val handler = Handler(this, notification, androidCallManager, pjsip, localBroadcastManager)
    val sounds: Sounds by inject()
    val middleware = Middleware(this)

    override fun onCreate() {
        super.onCreate()

        monitor.start(this)
        connection = AndroidCallConnection(this)

        broadcastReceiverManager.apply {
            registerReceiverViaGlobalBroadcastManager(networkConnectivity, ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiverViaGlobalBroadcastManager(screenOffReceiver, Int.MAX_VALUE, Intent.ACTION_SCREEN_OFF)
            registerReceiverViaGlobalBroadcastManager(ipSwitchMonitor, ConnectivityManager.CONNECTIVITY_ACTION, SipLogHandler.NETWORK_UNAVAILABLE_BROADCAST)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            val action = Action.valueOf(intent.action ?: throw Exception("Unable to find action"))

            if (sipActionHandler.isForegroundAction(action)) {
                startForeground(notification.notification.notificationId, notification.notification.build())
                sipServiceActive = true
                pjsip.init(this)
                ipSwitchMonitor.init(this, pjsip.endpoint)
            }

            sipActionHandler.handle(action, intent)
        } catch (e: Exception) {
            logger.e("Failed to perform action based on intent, stopping service: " + e.message)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    /**
     * Display the incoming call to the user, this will launch a notification and if the user is currently in the app
     * will display the incoming call activity.
     *
     */
    fun showIncomingCallToUser() = calls.current?.let {
        sounds.incomingCallAlerts.start()

        notification.change(INCOMING)

        if (get().isApplicationVisible()) {
            notification.toNotification(INCOMING).build().fullScreenIntent.send()
        }
    }

    /**
     * Displaying the outgoing call ui to  the user.
     *
     */
    fun showOutgoingCallToUser() {
        if (currentCall is IncomingCall) return

        startActivity(AbstractCallActivity.createIntentForCallActivity(this))
        notification.change(OUTGOING)
    }

    /**
     * Register a call with the sip service.
     *
     */
    fun registerCall(call: SipCall) {
        this.calls.add(call)
    }

    /**
     * Removes the call from the list and deletes it. If there are no calls left stop
     * the service.
     * @param call
     */
    fun unregisterCall(call: SipCall) {
        calls.remove(call)
        call.delete()

        if (calls.isEmpty()) {
            stopSelf()
        }
    }

    /**
     * Completely clean-up the SipService and all related objects.
     *
     */
    override fun onDestroy() {
        logger.d("onDestroy")
        connection.destroy()
        sounds.silence()
        pjsip.destroy()
        broadcastReceiverManager.unregisterReceiver(networkConnectivity, screenOffReceiver, ipSwitchMonitor)
        sipServiceActive = false
        super.onDestroy()
    }

    /**
     * Launch the call activity for whatever call we currently have active.
     *
     */
    fun startCallActivityForCurrentCall() {
        if (calls.current == null) {
            logger.e("Unable to start call activity for current call as there is no current call")
            return
        }

        calls.current?.let {
            startActivity(AbstractCallActivity.createIntentForCallActivity(this))
        }
    }

    /**
     * Broadcast a call event, defaulting to a regular call updated event if nothing is provided.
     *
     */
    fun broadcast(event: Event = Event.CALL_UPDATED) = localBroadcastManager.sendBroadcast(Intent(event.name))

    /**
     * Check to see if we are currently in the middle of a call transfer.
     *
     * @return TRUE if we are currently transferring, otherwise false.
     */
    fun isTransferring() : Boolean = calls.size > 1

    /**
     * Class the be able to bind a activity to this service.
     */
    inner class SipServiceBinder : Binder() {
        val service: SipService
            get() = this@SipService
    }

    /**
     * This method is called when the user opens the multi-tasking menu and swipes/closes Vialer.
     *
     * @param rootIntent
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        logger.i("Stopping SipService as task has been removed")
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    companion object {
        /**
         * Set when the SipService is active. This is used to respond to the middleware.
         */
        var sipServiceActive = false

        lateinit var connection: AndroidCallConnection
    }
}
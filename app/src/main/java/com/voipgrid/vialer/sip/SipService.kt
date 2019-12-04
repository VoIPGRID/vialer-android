package com.voipgrid.vialer.sip

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.telecom.CallAudioState
import android.telecom.PhoneAccount
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.android.calling.AndroidCallConnection
import com.voipgrid.vialer.android.calling.AndroidCallManager
import com.voipgrid.vialer.android.calling.AndroidCallService
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.CallStatusReceiver
import com.voipgrid.vialer.logging.LogHelper
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.sip.SipLogHandler
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.notifications.call.AbstractCallNotification
import com.voipgrid.vialer.notifications.call.ActiveCallNotification
import com.voipgrid.vialer.notifications.call.DefaultCallNotification
import com.voipgrid.vialer.notifications.call.IncomingCallNotification
import com.voipgrid.vialer.sip.core.Action
import com.voipgrid.vialer.sip.core.CallStack
import com.voipgrid.vialer.sip.core.SipActionHandler
import com.voipgrid.vialer.sip.incoming.MiddlewareResponse
import com.voipgrid.vialer.sip.outgoing.OutgoingCallRinger
import com.voipgrid.vialer.sip.pjsip.Pjsip
import com.voipgrid.vialer.sip.pjsip.Pjsip.SipAccount
import com.voipgrid.vialer.sip.service.SipServiceMonitor
import com.voipgrid.vialer.sip.utils.BusyTone
import com.voipgrid.vialer.sip.utils.ScreenOffReceiver
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.PhoneNumberUtils
import org.koin.android.ext.android.inject
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.pjsua_call_flag

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
class SipService : Service(), CallStatusReceiver.Listener {

    /**
     * This will track whether this instance of SipService has ever handled a call,
     * if this is the case we can shut down the sip service immediately if we don't
     * have a call when onStartCommand is run.
     */
    private var mSipServiceHasHandledACall = false

    private val calls = CallStack()

    val hasCall: Boolean
        get() = calls.isNotEmpty()

    val currentCall: SipCall?
        get() = calls.current

    val firstCall: SipCall?
        get() = calls.initial

    val pendingMiddlewareResponses = ArrayList<MiddlewareResponse>()

    private val mBinder: IBinder = SipServiceBinder()

    var notification: AbstractCallNotification = DefaultCallNotification()
        private set

    private val callStatusReceiver = CallStatusReceiver(this)
    private val sipActionHandler = SipActionHandler(this)

    private val logger = Logger(this)
    private val broadcastReceiverManager: BroadcastReceiverManager by inject()
    private val networkConnectivity: NetworkConnectivity by inject()
    private val incomingCallAlerts: IncomingCallAlerts by inject()
    private val androidCallManager: AndroidCallManager by inject()
    private val ipSwitchMonitor: IpSwitchMonitor by inject()
    private val monitor: SipServiceMonitor by inject()
    private val screenOffReceiver: ScreenOffReceiver by inject()
    private val localBroadcastManager: LocalBroadcastManager by inject()
    val nativeCallManager: NativeCallManager by inject()
    val sipBroadcaster: SipBroadcaster by inject()
    val pjsip: Pjsip by inject()
    val busyTone: BusyTone by inject()
    val outgoingCallRinger: OutgoingCallRinger by inject()

    override fun onCreate() {
        super.onCreate()

        monitor.start(this)
        changeNotification(notification)
        connection = AndroidCallConnection(this)

        broadcastReceiverManager.apply {
            registerReceiverViaGlobalBroadcastManager(networkConnectivity, ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiverViaLocalBroadcastManager(callStatusReceiver, SipConstants.ACTION_BROADCAST_CALL_STATUS)
            registerReceiverViaGlobalBroadcastManager(screenOffReceiver, Int.MAX_VALUE, Intent.ACTION_SCREEN_OFF)
            registerReceiverViaGlobalBroadcastManager(ipSwitchMonitor, ConnectivityManager.CONNECTIVITY_ACTION, SipLogHandler.NETWORK_UNAVAILABLE_BROADCAST)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        logger.i("mSipServiceHasHandledACall: $mSipServiceHasHandledACall")

        // If the SipService has already handled a call but now has no call, this suggests
        // that the SipService is stuck not doing anything so it should be immediately shut
        // down.
        if (mSipServiceHasHandledACall && calls.current == null) {
            logger.i("onStartCommand was triggered after a call has already been handled but with no current call, stopping SipService...")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val action = Action.valueOf(intent.action ?: throw Exception("Unable to find action"))

            if (sipActionHandler.isForegroundAction(action)) {
                startForeground(notification.notificationId, notification.build())
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
        incomingCallAlerts.start()

        val incomingCallNotification = notification.incoming(it.phoneNumber, it.callerId) as IncomingCallNotification

        changeNotification(incomingCallNotification)

        if (get().isApplicationVisible()) {
            incomingCallNotification.build().fullScreenIntent.send()
        }
    }

    /**
     * Initialise an outgoing call and either route it via the android call manager or display it immediately
     *
     * @param number The phone number to call.
     * @param isInvisible If set to true, the android call manager will be unaware of the call and no ui will be displayed. This is ideal
     * for transfers.
     *
     */
    fun placeOutgoingCall(number: String, isInvisible: Boolean = false) {
        if (calls.current != null && !isInvisible) {
            logger.i("Attempting to initialise a second outgoing call but this is not currently supported")
            startCallActivityForCurrentCall()
            return
        }

        val callback: (() -> Unit) = {
            val call = SipCall(this, pjsip.account).apply {
                phoneNumber = number
            }

            if (call.startOutgoingCall(number)) {
                this.calls.add(call)

                if (!isInvisible) {
                    showOutgoingCallToUser(call)
                }
            }
        }

        if (isInvisible) {
            Log.e("TEST123", "Placing an invisible call to $number")
            callback.invoke()
            return
        }

        AndroidCallService.outgoingCallback = callback
        androidCallManager.call(number)
    }

    /**
     * Displaying the outgoing call ui to  the user.
     *
     */
    private fun showOutgoingCallToUser(sipCall: SipCall) {
        startActivity(AbstractCallActivity.createIntentForCallActivity(this))
        changeNotification(notification.outgoing(sipCall))
    }

    /**
     * Completely clean-up the SipService and all related objects.
     *
     */
    override fun onDestroy() {
        logger.d("onDestroy")
        connection.destroy()
        silence()
        pjsip.destroy()
        sipBroadcaster.broadcastServiceInfo(SipConstants.SERVICE_STOPPED)
        broadcastReceiverManager.unregisterReceiver(networkConnectivity, callStatusReceiver, screenOffReceiver, ipSwitchMonitor)
        sipServiceActive = false
        super.onDestroy()
    }

    /**
     * Updates the notification and sets the active notification appropriately. All notification changes should be published
     * via this method.
     *
     * @param notification
     */
    fun changeNotification(notification: AbstractCallNotification) {
        logger.i("Received change notification request from: " + notification.javaClass.simpleName)
        if (shouldUpdateNotification(notification)) {
            logger.i("Performing notification change to" + notification.javaClass.simpleName)
            this.notification = notification
            startForeground(notification.notificationId, notification.build())
        }
    }

    /**
     * Check if the notification should be updated.
     *
     * @param notification
     * @return
     */
    private fun shouldUpdateNotification(notification: AbstractCallNotification): Boolean {
        if (this.notification.javaClass != notification.javaClass) return true

        if (notification.javaClass == ActiveCallNotification::class.java) {
            notification.display()
        }

        return false
    }

    /**
     * Removes the call from the list and deletes it. If there are no calls left stop
     * the service.
     * @param call
     */
    fun removeCallFromList(call: SipCall) {
        calls.remove(call)

        if (calls.isEmpty() || call.callIsTransferred) {
            stopSelf()
        }
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
     * Called when a call has connected.
     *
     */
    override fun onCallConnected() {
        val call = calls.current ?: return

        if (call.isOutgoing) {
            connection.setActive()
            return
        }

        logger.i("Call has connected, it is an inbound call so stop all incoming call notifications")
        startCallActivityForCurrentCall()
        silence()
        changeNotification(notification.active(call))
    }

    /**
     * When we receive an incoming call from out sip provider.
     *
     */
    fun onIncomingCall(incomingCallParam: OnIncomingCallParam, account: SipAccount) {
        val call = SipCall(this, account, incomingCallParam.callId, SipInvite(incomingCallParam.rdata.wholeMsg))

        if (currentCall != null || nativeCallManager.isBusyWithNativeCall) {
            LogHelper.using(logger).logBusyReason(this)
            call.busy()
            return
        }

        call.setupAsIncomingCall()

        calls.add(call)

        androidCallManager.incomingCall()
    }

    /**
     * Silence the incoming call alerts.
     *
     */
    fun silence() {
        incomingCallAlerts.stop()
    }

    /**
     * Android has reported that the call audio state has changed.
     *
     * @param state
     */
    fun onCallAudioStateChanged(state: CallAudioState) {
        Toast.makeText(get(), state.toString(), Toast.LENGTH_LONG).show()
        localBroadcastManager.sendBroadcast(Intent(Event.CALL_UPDATED.name))
    }

    /**
     * Pjsip has successfully registered with the server.
     *
     */
    fun onRegister() {
        logger.d("onAccountRegistered")
        respondToMiddleware()

        val call = calls.current ?: return

        if (call.isIpChangeInProgress && call.currentCallState == SipConstants.CALL_INCOMING_RINGING) {
            try {
                call.reinvite(CallOpParam().apply {
                    options = pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT.swigValue().toLong()
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Respond to the middleware with the details for the incoming call.
     *
     */
    private fun respondToMiddleware() {
        pendingMiddlewareResponses.forEach {
            MiddlewareHelper.respond(it.token, it.startTime)
        }

        this.pendingMiddlewareResponses.clear()
    }

    /**
     * Reinvite the current call.
     *
     */
    fun reinvite() {
        val call = calls.current ?: return

        if (!call.isIpChangeInProgress) {
            try {
                call.reinvite(CallOpParam(true))
            } catch (e: Exception) {
                logger.e("Unable to reinvite call")
            }
        }
    }

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

    companion object {
        /**
         * Set when the SipService is active. This is used to respond to the middleware.
         */
        var sipServiceActive = false

        lateinit var connection: AndroidCallConnection
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    /**
     * The extras (i.e. parameters) that can be passed to the SipService when starting it.
     *
     */
    enum class Extra {
        OUTGOING_PHONE_NUMBER,
        OUTGOING_CONTACT_NAME,
        INCOMING_TOKEN,
        INCOMING_CALL_START_TIME
    }

    /**
     * The actions (i.e. events) that will be broadcast from the SipService.
     *
     */
    enum class Event {
        CALL_UPDATED
    }
}
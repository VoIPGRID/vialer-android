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
import com.voipgrid.vialer.android.calling.AndroidCallService
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.calling.AbstractCallActivity
import com.voipgrid.vialer.calling.CallStatusReceiver
import com.voipgrid.vialer.logging.LogHelper
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.sip.SipLogHandler
import com.voipgrid.vialer.middleware.MiddlewareHelper
import com.voipgrid.vialer.sip.SipCall.TelephonyState.*
import com.voipgrid.vialer.sip.core.Action
import com.voipgrid.vialer.sip.core.CallStack
import com.voipgrid.vialer.sip.core.SipActionHandler
import com.voipgrid.vialer.sip.incoming.MiddlewareResponse
import com.voipgrid.vialer.sip.outgoing.OutgoingCallRinger
import com.voipgrid.vialer.sip.pjsip.Pjsip
import com.voipgrid.vialer.sip.pjsip.Pjsip.SipAccount
import com.voipgrid.vialer.sip.service.SipServiceMonitor
import com.voipgrid.vialer.sip.service.SipServiceNotificationManager
import com.voipgrid.vialer.sip.service.SipServiceNotificationManager.Type.*
import com.voipgrid.vialer.sip.transfer.CallTransferResult
import com.voipgrid.vialer.sip.utils.BusyTone
import com.voipgrid.vialer.sip.utils.ScreenOffReceiver
import com.voipgrid.vialer.util.BroadcastReceiverManager
import org.koin.android.ext.android.inject
import org.pjsip.pjsua2.AudioMedia
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

    private val callStatusReceiver = CallStatusReceiver(this)
    private val sipActionHandler = SipActionHandler(this)
    private val notification = SipServiceNotificationManager(this)

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


            startForeground(notification.active.notificationId, notification.active.build())
            if (sipActionHandler.isForegroundAction(action)) {
//                startForeground(notification.active.notificationId, notification.active.build())
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

        notification.change(INCOMING)

        if (get().isApplicationVisible()) {
            notification.toNotification(INCOMING).build().fullScreenIntent.send()
        }
    }

    /**
     * Initialise an outgoing call and either route it via the android call manager or display it immediately
     *
     * @param number The phone number to call.
     *
     */
    fun placeOutgoingCall(number: String) {
        if (calls.current != null) {
            logger.i("Attempting to initialise a second outgoing call but this is not currently supported")
            startCallActivityForCurrentCall()
            return
        }

        AndroidCallService.outgoingCallback = {
            makeCall(number)
            showOutgoingCallToUser()
        }

        androidCallManager.call(number)
    }

    private fun makeCall(number: String) = SipCall(this, pjsip.account, number).also { this.calls.add(it) }

    /**
     * Start a transfer to the target number.
     *
     */
    fun startTransfer(target: String) {
        if (isTransferring()) throw Exception("Unable to start transfer when there is already one in progress")

        makeCall(target)
    }

    /**
     * Merge the current transfer.
     *
     */
    fun mergeTransfer(): CallTransferResult {
        if (!isTransferring()) throw Exception("Unable to merge transfer that has not started")

        val firstCall = firstCall ?: throw Exception("Must have a first call to transfer")
        val currentCall = currentCall ?: throw Exception("Must have a current call to transfer")

        val result = CallTransferResult(firstCall.phoneNumber, currentCall.phoneNumber)

        firstCall.xFerReplaces(currentCall)

        return result
    }

    /**
     * Displaying the outgoing call ui to  the user.
     *
     */
    private fun showOutgoingCallToUser() {
        startActivity(AbstractCallActivity.createIntentForCallActivity(this))
        notification.change(OUTGOING)
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
     * Removes the call from the list and deletes it. If there are no calls left stop
     * the service.
     * @param call
     */
    private fun removeCallFromList(call: SipCall) {
        calls.remove(call)
        call.delete()

        if (calls.isEmpty()) { //todo  || call.state.callIsTransferred
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

    fun onTelephonyStateChange(call: SipCall, state: SipCall.TelephonyState): Unit = when(state) {
        INITIALIZING -> TODO()
        INCOMING_RINGING -> TODO()
        OUTGOING_RINGING -> outgoingCallRinger.start()
        CONNECTED -> {
            outgoingCallRinger.stop()
            silence()
            notification.change(ACTIVE)
            when {
                call.isOutgoing -> connection.setActive()
                call.isIncoming -> startCallActivityForCurrentCall()
                else -> {}
            }
        }
        DISCONNECTED -> {
            if (!isTransferring() && !call.state.wasUserHangup) {
                busyTone.play()
            }

            removeCallFromList(call)
        }
    }.also {
        localBroadcastManager.sendBroadcast(Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS).apply {
            putExtra(SipConstants.CALL_STATUS_KEY, state.name)
        })
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

        call.beginIncomingRinging()

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
     * This will hangup the correct call.
     *
     */
    fun hangup() {
        if (isTransferring()) {
            currentCall?.hangup(true)
            return
        }

        connection.onDisconnect()
    }

    /**
     * Android has reported that the call audio state has changed.
     *
     * @param state
     */
    fun onCallAudioStateChanged(state: CallAudioState) {
        Toast.makeText(get(), state.toString(), Toast.LENGTH_LONG).show()
        broadcastCallUpdate()
    }

    private fun broadcastCallUpdate() = localBroadcastManager.sendBroadcast(Intent(Event.CALL_UPDATED.name))

    /**
     * Pjsip has successfully registered with the server.
     *
     */
    fun onRegister() {
        logger.d("onAccountRegistered")
        respondToMiddleware()

        val call = calls.current ?: return

        if (call.state.isIpChangeInProgress && call.state.telephonyState == INCOMING_RINGING) {
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

        if (!call.state.isIpChangeInProgress) {
            try {
                call.reinvite(CallOpParam(true))
            } catch (e: Exception) {
                logger.e("Unable to reinvite call")
            }
        }
    }

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
     * The call has let us know that its audio should be connected.
     *
     */
    fun beginTransmittingAudio() {
        outgoingCallRinger.stop()

        val call = currentCall ?: throw Exception("Unable to begin trasnmitting audio with no call")
        val endpoint = pjsip.endpoint ?: throw Exception("Unable to connect audio with no endpoint")

        val media = AudioMedia.typecastFromMedia(call.getUsableAudio())
        val audDevManager = endpoint.audDevManager()
        media.startTransmit(audDevManager.playbackDevMedia)
        audDevManager.captureDevMedia.startTransmit(media)
    }

    fun callWasMissed(sipCall: SipCall) {
        logger.i("A call was missed...")
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
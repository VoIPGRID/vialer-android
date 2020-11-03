package com.voipgrid.vialer.fcm

import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.Middleware
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.logging.LogHelper
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.notifications.VoipDisabledNotification
import com.voipgrid.vialer.phonelib.Initialiser
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.SipUri
import com.voipgrid.vialer.statistics.VialerStatistics
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

/**
 * Listen to messages from FCM. The backend server sends us FCM notifications when we have
 * incoming calls.
 */
class FcmMessagingService : FirebaseMessagingService(), KoinComponent {

    private val logger = Logger(this)
    private val connectivityHelper: ConnectivityHelper by inject()
    private val powerManager: PowerManager by inject()
    private val nativeCallManager: NativeCallManager by inject()
    private val middlewareApi: Middleware by inject()
    private val middleware: com.voipgrid.vialer.middleware.Middleware by inject()
    private val initialiser: Initialiser by inject()

    /**
     * The number of times the middleware will attempt to send a push notification
     * before it gives up and the string stores the last call we have SUCCESSFULLY handled and
     * started the SipService for.
     *
     */
    companion object {
        private const val MAX_MIDDLEWARE_PUSH_ATTEMPTS = 8
        private var lastHandledCall: String? = null
        const val VOIP_HAS_BEEN_DISABLED = "com.voipgrid.vialer.voip_disabled"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val remoteMessageData = RemoteMessageData(remoteMessage.data)
        LogHelper.using(logger).logMiddlewareMessageReceived(remoteMessage, remoteMessageData.requestType)
        VialerStatistics.pushNotificationWasReceived(remoteMessage)

        when {
            !remoteMessageData.hasRequestType() -> logger.e("No requestType")
            remoteMessageData.isCallRequest -> handleCall(remoteMessage, remoteMessageData)
            remoteMessageData.isMessageRequest -> handleMessage(remoteMessage, remoteMessageData)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        logger.d("Message deleted on the FCM server.")
    }

    /**
     * Handle a push message with a call request type.
     *
     * @param remoteMessage
     * @param remoteMessageData
     */
    private fun handleCall(remoteMessage: RemoteMessage, remoteMessageData: RemoteMessageData) {
        logCurrentState(remoteMessageData)

        when {
            !isConnectionSufficient() -> handleInsufficientConnection(remoteMessage, remoteMessageData)
            isAVialerCallAlreadyInProgress() -> rejectDueToVialerCallAlreadyInProgress(remoteMessage, remoteMessageData)
            nativeCallManager.isBusyWithNativeCall ->  rejectDueToNativeCallAlreadyInProgress(remoteMessage, remoteMessageData)
            else -> {


                if (lastHandledCall != remoteMessageData.requestToken) {
                    logger.d("Payload processed, calling startService method")
                    lastHandledCall = remoteMessageData.requestToken
                    startSipService(remoteMessageData)
                }
            }
        }
    }

    /**
     * Handle a push message with a message request type.
     *
     * @param remoteMessage
     * @param remoteMessageData
     */
    private fun handleMessage(remoteMessage: RemoteMessage, remoteMessageData: RemoteMessageData) {

        if (!remoteMessageData.isRegisteredOnOtherDeviceMessage) {
            return
        }

        if (User.voip.hasEnabledSip) {
            VoipDisabledNotification().display()
            User.voip.hasEnabledSip = false
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(VOIP_HAS_BEEN_DISABLED))
    }

    /**
     * Performs various tasks that are required when we are rejecting a call due to an insufficient
     * network connection.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private fun handleInsufficientConnection(remoteMessage: RemoteMessage, remoteMessageData: RemoteMessageData) {
        if (hasExceededMaximumAttempts(remoteMessageData)) {
            VialerStatistics.incomingCallFailedDueToInsufficientNetwork(remoteMessage)
        }
        logger.e(when(isDeviceInIdleMode()){
            true -> "Device in idle mode and connection insufficient. For now do nothing wait for next middleware push."
            false -> "Connection is insufficient. For now do nothing and wait for next middleware push"
        })
    }

    /**
     * Check if we have a good enough connection to accept an incoming call.
     *
     * @return TRUE if we have a good enough connection, otherwise FALSE.
     */
    private fun isConnectionSufficient() = connectivityHelper.hasNetworkConnection() && connectivityHelper.hasFastData()

    /**
     * Check to see if the SIP service is currently running, this means that there is already a call
     * in progress and we can not accept further calls.
     *
     * @return TRUE if there is an active call, otherwise FALSE
     */
    private fun isAVialerCallAlreadyInProgress() = SipService.sipServiceActive;

    /**
     * Check if we have reached or exceeded the maximum number of attempts that we
     * accept from the middleware.
     *
     * @param remoteMessageData The remote message data that we are handling.
     * @return TRUE if we have reached or exceeded maximum attempts, otherwise FALSE.
     */
    private fun hasExceededMaximumAttempts(remoteMessageData: RemoteMessageData) = remoteMessageData.getAttemptNumber() >= MAX_MIDDLEWARE_PUSH_ATTEMPTS

    /**
     * Performs various tasks that are necessary when rejecting a call based on the fact that there is
     * already a Vialer call in progress.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private fun rejectDueToVialerCallAlreadyInProgress(remoteMessage: RemoteMessage, remoteMessageData: RemoteMessageData) {
        logger.d("Reject due to call already in progress")

        replyServer(remoteMessageData, false)

        sendCallFailedDueToOngoingVialerCallMetric(remoteMessage, remoteMessageData.requestToken)
    }

    /**
     * Performs various tasks that are necessary when rejecting a call based on the fact that there is
     * already a Vialer call in progress.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private fun rejectDueToNativeCallAlreadyInProgress(remoteMessage: RemoteMessage, remoteMessageData: RemoteMessageData) {
        logger.d("Reject due to native call already in progress")

        replyServer(remoteMessageData, false)

        VialerStatistics.incomingCallFailedDueToOngoingGsmCall(remoteMessage)
    }

    /**
     * Send the vialer metric for ongoing call if appropriate.
     *
     * @param remoteMessage
     * @param requestToken
     */
    private fun sendCallFailedDueToOngoingVialerCallMetric(remoteMessage: RemoteMessage, requestToken: String) {
        if (lastHandledCall != null && lastHandledCall == requestToken) {
            logger.i("Push notification ($lastHandledCall) is being rejected because there is a Vialer call already in progress but not sending metric because it was already handled successfully")
            return
        }

        VialerStatistics.incomingCallFailedDueToOngoingVialerCall(remoteMessage)
    }

    /**
     * Notify the middleware server that we are, in fact, alive.
     *
     * @param remoteMessageData The remote message data from the remote message that we are handling.
     * @param isAvailable TRUE if the phone is ready to accept the incoming call, FALSE if it is not available.
     */
    private fun replyServer(remoteMessageData: RemoteMessageData, isAvailable: Boolean) = GlobalScope.launch {
        val response =  middlewareApi.reply(
                remoteMessageData.requestToken,
                isAvailable,
                remoteMessageData.messageStartTime,
                User.voipAccount?.accountId
        ).execute()
        if (response.isSuccessful) {
            logger.i("response was successful")
        }
    }

    /**
     * Start the SIP service with the relevant data from the push message in the
     * intent.
     *
     * @param remoteMessageData
     */
    private fun startSipService(remoteMessageData: RemoteMessageData) {
        val intent = Intent(this, SipService::class.java).apply {
            action = SipService.Actions.HANDLE_INCOMING_CALL
            data = SipUri.sipAddressUri(this@FcmMessagingService,PhoneNumberUtils.format(remoteMessageData.phoneNumber))
            putExtra(SipConstants.EXTRA_RESPONSE_URL, remoteMessageData.responseUrl);
            putExtra(SipConstants.EXTRA_REQUEST_TOKEN, remoteMessageData.requestToken)
            putExtra(SipConstants.EXTRA_PHONE_NUMBER, remoteMessageData.phoneNumber)
            putExtra(SipConstants.EXTRA_CONTACT_NAME, remoteMessageData.callerId)
            putExtra(RemoteMessageData.MESSAGE_START_TIME, remoteMessageData.messageStartTime)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * Device can be in Idle mode when it's been idling to long. This means that network connectivity
     * is reduced. So we check if we are in that mode and the connection is insufficient.
     * Just return and don't reply to the middleware for now.
     *
     * @return
     */
    private fun isDeviceInIdleMode() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager.isDeviceIdleMode

    /**
     * Log some information about our current state to help determine what state the phone is in when
     * a push notification is incoming.
     *
     * @param remoteMessageData
     */
    private fun logCurrentState(remoteMessageData: RemoteMessageData) {
        listOf(
                "SipService Active: " + SipService.sipServiceActive,    "CurrentConnection: " + connectivityHelper.getConnectionTypeString(),
                "Payload: " + remoteMessageData.getRawData().toString()
        ).forEach{
            logger.d(it)
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        middleware.register()
    }
}


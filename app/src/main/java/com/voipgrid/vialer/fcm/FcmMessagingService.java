package com.voipgrid.vialer.fcm;



import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.api.Middleware;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;
import com.voipgrid.vialer.notifications.VoipDisabledNotification;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.sip.core.Action;
import com.voipgrid.vialer.sip.service.SipServiceExtra;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Listen to messages from FCM. The backend server sends us FCM notifications when we have
 * incoming calls.
 */
public class FcmMessagingService extends FirebaseMessagingService {

    /**
     * The number of times the middleware will attempt to send a push notification
     * before it gives up.
     *
     */
    private static final int MAX_MIDDLEWARE_PUSH_ATTEMPTS = 8;

    /**
     * Stores the last call we have SUCCESSFULLY handled and started the SipService
     * for.
     */
    private static String sLastHandledCall;

    public static final String VOIP_HAS_BEEN_DISABLED = "com.voipgrid.vialer.voip_disabled";

    private Logger mRemoteLogger;
    private ConnectivityHelper mConnectivityHelper;
    private PowerManager mPowerManager;
    private NativeCallManager nativeCallManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mRemoteLogger = new Logger(FcmMessagingService.class);
        mConnectivityHelper = ConnectivityHelper.get(this);
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        nativeCallManager = new NativeCallManager((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
        mRemoteLogger.d("onCreate");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        mRemoteLogger.d("onMessageReceived");
        RemoteMessageData remoteMessageData = new RemoteMessageData(remoteMessage.getData());
        LogHelper.using(mRemoteLogger).logMiddlewareMessageReceived(remoteMessage, remoteMessageData.getRequestType());

        if (!remoteMessageData.hasRequestType()) {
            mRemoteLogger.e("No requestType");
            return;
        }

        if (remoteMessageData.isCallRequest()) {
            handleCall(remoteMessage, remoteMessageData);
            return;
        }

        if (remoteMessageData.isMessageRequest()) {
            handleMessage(remoteMessage, remoteMessageData);
            return;
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        mRemoteLogger.d("Message deleted on the FCM server.");
    }

    /**
     * Handle a push message with a call request type.
     *
     * @param remoteMessage
     * @param remoteMessageData
     */
    private void handleCall(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        logCurrentState(remoteMessageData);

        if (!isConnectionSufficient()) {
            handleInsufficientConnection(remoteMessage, remoteMessageData);
            return;
        }

        if (isAVialerCallAlreadyInProgress()) {
            rejectDueToVialerCallAlreadyInProgress(remoteMessage, remoteMessageData);
            return;
        }

        if (nativeCallManager.isBusyWithNativeCall()) {
            rejectDueToNativeCallAlreadyInProgress(remoteMessage, remoteMessageData);
            return;
        }

        sLastHandledCall = remoteMessageData.getRequestToken();

        mRemoteLogger.d("Payload processed, calling startService method");

        startSipService(remoteMessageData);
    }

    /**
     * Handle a push message with a message request type.
     *
     * @param remoteMessage
     * @param remoteMessageData
     */
    private void handleMessage(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        if (!remoteMessageData.isRegisteredOnOtherDeviceMessage()) {
            return;
        }

        if (User.voip.getHasEnabledSip()) {
            new VoipDisabledNotification().display();
        }

        User.voip.setHasEnabledSip(false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(VOIP_HAS_BEEN_DISABLED));
    }

    /**
     * Performs various tasks that are required when we are rejecting a call due to an insufficient
     * network connection.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private void handleInsufficientConnection(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        if (isDeviceInIdleMode()) {
            mRemoteLogger.e("Device in idle mode and connection insufficient. For now do nothing wait for next middleware push.");
        }
        else {
            mRemoteLogger.e("Connection is insufficient. For now do nothing and wait for next middleware push");
        }
    }

    /**
     * Check if we have a good enough connection to accept an incoming call.
     *
     * @return TRUE if we have a good enough connection, otherwise FALSE.
     */
    private boolean isConnectionSufficient() {
        return mConnectivityHelper.hasNetworkConnection() && mConnectivityHelper.hasFastData();
    }

    /**
     * Check to see if the SIP service is currently running, this means that there is already a call
     * in progress and we can not accept further calls.
     *
     * @return TRUE if there is an active call, otherwise FALSE
     */
    private boolean isAVialerCallAlreadyInProgress() {
        return SipService.Companion.getSipServiceActive();
    }

    /**
     * Check if we have reached or exceeded the maximum number of attempts that we
     * accept from the middleware.
     *
     * @param remoteMessageData The remote message data that we are handling.
     * @return TRUE if we have reached or exceeded maximum attempts, otherwise FALSE.
     */
    private boolean hasExceededMaximumAttempts(RemoteMessageData remoteMessageData) {
        return remoteMessageData.getAttemptNumber() >= MAX_MIDDLEWARE_PUSH_ATTEMPTS;
    }

    /**
     * Performs various tasks that are necessary when rejecting a call based on the fact that there is
     * already a Vialer call in progress.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private void rejectDueToVialerCallAlreadyInProgress(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("Reject due to call already in progress");

        rejectCall(remoteMessageData);

        sendCallFailedDueToOngoingVialerCallMetric(remoteMessage, remoteMessageData.getRequestToken());
    }

    /**
     * Performs various tasks that are necessary when rejecting a call based on the fact that there is
     * already a Vialer call in progress.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private void rejectDueToNativeCallAlreadyInProgress(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("Reject due to native call already in progress");

        rejectCall(remoteMessageData);
    }

    /**
     * Send the vialer metric for ongoing call if appropriate.
     *
     * @param remoteMessage
     * @param requestToken
     */
    private void sendCallFailedDueToOngoingVialerCallMetric(RemoteMessage remoteMessage, String requestToken) {
        if (sLastHandledCall != null && sLastHandledCall.equals(requestToken)) {
            mRemoteLogger.i("Push notification (" + sLastHandledCall + ") is being rejected because there is a Vialer call already in progress but not sending metric because it was already handled successfully");
            return;
        }
    }

    /**
     * Notify the middleware server that we are, in fact, alive.
     *
     * @param remoteMessageData The remote message data from the remote message that we are handling.
     */
    private void rejectCall(RemoteMessageData remoteMessageData) {
        if (remoteMessageData.getRequestToken().equals(sLastHandledCall)) return;

        mRemoteLogger.d("replyServer");
        ServiceGenerator.createRegistrationService(this)
                .reply(remoteMessageData.getRequestToken(), false, remoteMessageData.getMessageStartTime())
                .enqueue(null);
    }

    /**
     * Start the SIP service with the relevant data from the push message in the
     * intent.
     *
     * @param remoteMessageData
     */
    private void startSipService(RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("startSipService");
        Intent intent = new Intent(this, SipService.class);
        intent.setAction(Action.HANDLE_INCOMING_CALL.toString());

        // Set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                this,
                PhoneNumberUtils.format(remoteMessageData.getPhoneNumber())
        );

        intent.setData(sipAddressUri);

        intent.putExtra(SipServiceExtra.INCOMING_TOKEN.name(), remoteMessageData.getRequestToken());
        intent.putExtra(SipServiceExtra.INCOMING_CALL_START_TIME.name(), remoteMessageData.getMessageStartTime());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    /**
     * Device can ben in Idle mode when it's been idling to long. This means that network connectivity
     * is reduced. So we check if we are in that mode and the connection is insufficient.
     * just return and don't reply to the middleware for now.
     *
     * @return
     */
    private boolean isDeviceInIdleMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mPowerManager.isDeviceIdleMode();
    }

    /**
     * Log some information about our current state to help determine what state the phone is in when
     * a push notification is incoming.
     *
     * @param remoteMessageData
     */
    private void logCurrentState(RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("SipService Active: " + SipService.Companion.getSipServiceActive());
        mRemoteLogger.d("CurrentConnection: " + mConnectivityHelper.getConnectionTypeString());
        mRemoteLogger.d("Payload: " + remoteMessageData.getRawData().toString());
    }

    @Override
    public void onNewToken(final String s) {
        super.onNewToken(s);
        mRemoteLogger.d("onTokenRefresh");
        MiddlewareHelper.setRegistrationStatus(
                com.voipgrid.vialer.persistence.Middleware.RegistrationStatus.UNREGISTERED);
        MiddlewareHelper.registerAtMiddleware(this);
    }
}
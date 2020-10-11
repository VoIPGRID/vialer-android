package com.voipgrid.vialer.sip;

import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;
import static com.voipgrid.vialer.sip.SipConstants.BUSY_TONE_DURATION;
import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_IDENTIFIER_KEY;
import static com.voipgrid.vialer.sip.SipConstants.CALL_STATUS_CODE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_STATUS_KEY;
import static com.voipgrid.vialer.sip.SipConstants.EXTRA_PHONE_NUMBER;

import static org.koin.java.KoinJavaComponent.get;
import static org.koin.java.KoinJavaComponent.inject;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.CallStatisticsUpdater;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.audio.AudioRouter;
import com.voipgrid.vialer.bluetooth.AudioStateChangeReceiver;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.CallStatusReceiver;
import com.voipgrid.vialer.calling.CallingConstants;
import com.voipgrid.vialer.dialer.ToneGenerator;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.notifications.call.AbstractCallNotification;
import com.voipgrid.vialer.notifications.call.ActiveCallNotification;
import com.voipgrid.vialer.notifications.call.DefaultCallNotification;
import com.voipgrid.vialer.notifications.call.IncomingCallNotification;
import com.voipgrid.vialer.notifications.call.MissedCallNotification;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.phonelib.SessionExtensionsKt;
import com.voipgrid.vialer.phonelib.SoftPhone;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import org.jetbrains.annotations.NotNull;
import org.openvoipalliance.phonelib.PhoneLib;
import org.openvoipalliance.phonelib.model.Reason;
import org.openvoipalliance.phonelib.model.Session;
import org.openvoipalliance.phonelib.repository.initialise.SessionCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import kotlin.Lazy;

import com.voipgrid.vialer.phonelib.SessionExtensionsKt.*;

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
public class SipService extends Service implements SipServiceTic.TicListener {

    private SoftPhone softphone = get(SoftPhone.class);

    /**
     * This will track whether this instance of SipService has ever handled a call,
     * if this is the case we can shut down the sip service immediately if we don't
     * have a call when onStartCommand is run.
     */
    private boolean mSipServiceHasHandledACall = false;

    /**
     * Set when the SipService is active. This is used to respond to the middleware.
     */
    public static boolean sipServiceActive = false;

    public boolean incomingAlertsMuted = false;

    private Intent mIncomingCallDetails = null;

    @Nullable private Intent intent;

    private Logger mLogger;
    private final BroadcastReceiver phoneStateReceiver = new PhoneStateReceiver();
    private Runnable mRingbackRunnable = new OutgoingCallRinger();
    private final IBinder mBinder = new SipServiceBinder();
    private CheckServiceIsRunning mCheckService = new CheckServiceIsRunning();
    private AbstractCallNotification callNotification = new DefaultCallNotification();
    private AbstractCallNotification activeNotification;
    private ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver();
    private SipServiceTic tic = new SipServiceTic(this);
    private final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

    private CallStatisticsUpdater callStatisticsUpdater = new CallStatisticsUpdater();
    private CallStatusReceiver statsUpdaterCallStatusReceiver = new CallStatusReceiver(
            callStatisticsUpdater
    );

    @Inject protected SipConfig mSipConfig;
    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;
    @Inject protected Handler mHandler;
    @Inject protected ToneGenerator mToneGenerator;
    @Inject protected NetworkConnectivity mNetworkConnectivity;
    @Inject protected NativeCallManager mNativeCallManager;
    @Inject
    @Nullable
    protected PhoneAccount mPhoneAccount;
    @Inject AudioRouter audioRouter;
    @Inject IncomingCallAlerts incomingCallAlerts;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLogger = new Logger(SipService.class);
        VialerApplication.get().component().inject(this);
        AudioStateChangeReceiver.fetch();
        mLogger.d("onCreate");

        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(
                phoneStateReceiver,
                TelephonyManager.ACTION_PHONE_STATE_CHANGED
        );
        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(
                mNetworkConnectivity,
                ConnectivityManager.CONNECTIVITY_ACTION
        );
        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(
                statsUpdaterCallStatusReceiver,
                ACTION_BROADCAST_CALL_STATUS
        );
        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(
                screenOffReceiver,
                Integer.MAX_VALUE,
                Intent.ACTION_SCREEN_OFF
        );
        mCheckService.start();
        changeNotification(callNotification);
        tic.begin();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLogger.i("mSipServiceHasHandledACall: " + mSipServiceHasHandledACall);

        // If the SipService has already handled a call but now has no call, this suggests
        // that the SipService is stuck not doing anything so it should be immediately shut
        // down.
        if (mSipServiceHasHandledACall && !softphone.getHasCall()) {
            mLogger.i(
                    "onStartCommand was triggered after a call has already been handled but with no current call, stopping SipService...");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            boolean shouldStartForeground = performActionBasedOnIntent(intent);

            if (shouldStartForeground && activeNotification == null) {
                startForeground(callNotification.getNotificationId(), callNotification.build());
            }
        } catch (Exception e) {
            mLogger.e("Failed to perform action based on intent, stopping service: "
                    + e.getMessage());
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    /**
     * Performs an action based on the action in the intent that was used to start
     * the SipService.
     *
     * @param intent
     */
    public boolean performActionBasedOnIntent(Intent intent) throws SecurityException {
        if (intent == null) return false;

        this.intent = intent;
        final String action = "" + this.intent.getAction();

        mLogger.i("Performing action: " + action);

        if (Actions.HANDLE_INCOMING_CALL.equals(action)) {
            initialiseIncomingCall();
            return true;
        } else if (Actions.HANDLE_OUTGOING_CALL.equals(action)) {
            initialiseOutgoingCall(intent);
            return true;
        } else if (Actions.DECLINE_INCOMING_CALL.equals(action)) {
            incomingCallAlerts.stop();
            softphone.getPhone().declineIncoming(softphone.getCall(), Reason.DECLINED);
        } else if (Actions.ANSWER_INCOMING_CALL.equals(action)) {
            if (!MicrophonePermission.hasPermission(VialerApplication.get())) {
                Toast.makeText(this, getString(R.string.permission_microphone_missing_message),
                        Toast.LENGTH_LONG).show();
                mLogger.e("Unable to answer incoming call as we do not have microphone permission");
                return false;
            }

            incomingCallAlerts.stop();
            softphone.getPhone().acceptIncoming(softphone.getCall());
        } else if (Actions.END_CALL.equals(action)) {
            softphone.getPhone().end(softphone.getCall());
        } else if (Actions.ANSWER_OR_HANGUP.equals(action)) {
            if (SessionExtensionsKt.isRinging(softphone.getCall())) {
                softphone.getPhone().acceptIncoming(softphone.getCall());
            } else if (SessionExtensionsKt.isConnected(softphone.getCall())) {
                softphone.getPhone().end(softphone.getCall());
            }
        } else if (Actions.DISPLAY_CALL_IF_AVAILABLE.equals(action)) {
            if (getCurrentCall() != null) {
                if (SessionExtensionsKt.isConnected(softphone.getCall())) {
                    startCallActivityForCurrentCall();
                }
            } else {
                stopSelf();
            }
        } else {
            mLogger.e("SipService received an invalid action: " + action);
        }

        return false;
    }

    /**
     * Perform necessary steps to initialise an incoming call.
     *
     */
    private void initialiseIncomingCall() {
        mLogger.d("incomingCall");
        mIncomingCallDetails = intent;
        loadSip();
        performPostCallCreationActions();
    }

    /**
     * Perform necessary steps to initialise an outgoing call.
     *
     */
    private void initialiseOutgoingCall(Intent intent) {
        if (getCurrentCall() != null) {
            getLogger().i(
                    "Attempting to initialise a second outgoing call but this is not currently supported");
            startCallActivityForCurrentCall();
            return;
        }

        mLogger.d("outgoingCall");
        loadSip();
        makeCall(intent.getStringExtra(EXTRA_PHONE_NUMBER), true);
        performPostCallCreationActions();
    }

    /**
     * Actions to perform when a call has been created, that being outgoing or incoming.
     *
     */
    private void performPostCallCreationActions() {
        if (audioRouter.isBluetoothRouteAvailable()
                && !audioRouter.isCurrentlyRoutingAudioViaBluetooth()) {
            audioRouter.routeAudioViaBluetooth();
        }
    }

    /**
     * Begin the process of actually starting the SIP library so we can
     * start/receive calls.
     *
     */
    private void loadSip() {
        if (mPhoneAccount == null) {
            mLogger.w("No sip account when trying to create service");
            stopSelf();
            return;
        }

        try {
            mLogger.i("Attempting to load sip lib");
            mSipConfig = mSipConfig.init(
                    this,
                    mPhoneAccount,
                    intent != null && Actions.HANDLE_INCOMING_CALL.equals(intent.getAction())
            );
            mSipConfig.initLibrary(callback);
        } catch (Exception e) {
            Log.e("TEST123", "", e);
            mLogger.e("Failed to load pjsip, stopping the service");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mLogger.d("onDestroy");

        audioRouter.destroy();
        incomingCallAlerts.stop();
        tic.stop();

        mBroadcastReceiverManager.unregisterReceiver(
                phoneStateReceiver,
                mNetworkConnectivity,
                statsUpdaterCallStatusReceiver,
                screenOffReceiver
        );

        mHandler.removeCallbacks(mCheckService);

        sipServiceActive = false;
        softphone.cleanUp();
        super.onDestroy();
    }

    /**
     * Play the busy tone used when a call get's disconnected by the recipient.
     */
    public void playBusyTone() {
        try {
            mToneGenerator.startTone(ToneGenerator.Constants.TONE_CDMA_NETWORK_BUSY,
                    BUSY_TONE_DURATION);
            Thread.sleep(BUSY_TONE_DURATION);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Start the ring back for a outgoing call.
     */
    public void startRingback() {
        mLogger.d("onCallStartRingback");
        audioRouter.focus();
        mHandler.postDelayed(mRingbackRunnable, 2000);
    }

    /**
     * Stop the ring back for a outgoing call.
     */
    public void stopRingback() {
        mLogger.d("onCallStopRingback");
        mHandler.removeCallbacks(mRingbackRunnable);
    }

    public void makeCall(String phoneNumber) {
        makeCall(phoneNumber, false);
    }

    /**
     * Function to make a call with or without starting a activity.
     * @param phoneNumber
     * @param startActivity
     */
    public void makeCall(String phoneNumber, boolean startActivity) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            PhoneLib.getInstance(this).callTo(phoneNumber);

            if (startActivity) {
                startOutgoingCallActivity(phoneNumber);
            }
        }

    }

    /**
     * Start the activity for a (initial) outgoing call.
     * @param number
     */
    public void startOutgoingCallActivity(String number) {
        startCallActivity(
                number,
                CallingConstants.TYPE_OUTGOING_CALL,
                CallActivity.class
        );

        changeNotification(callNotification.outgoing(softphone.getCall()));
    }

    /**
     * Start the activity for a incoming call.
     * @param number
     * @param callerId
     */
    public void informUserAboutIncomingCall(String number, String callerId) {
        changeNotification(callNotification.incoming(number, callerId));
        incomingCallAlerts.start();
    }

    /**
     * Updates the notification and sets the active notification appropriately. All notification changes should be published
     * via this method.
     *
     * @param notification
     */
    public void changeNotification(final @NonNull AbstractCallNotification notification) {
        mLogger.i("Received change notification request from: " + notification.getClass().getSimpleName());

        if (shouldUpdateNotification(notification)) {
            mLogger.i("Performing notification change to" + notification.getClass().getSimpleName());
            activeNotification = notification;
            startForeground(notification.getNotificationId(), notification.build());
            launchIncomingCallActivityWhenAppIsVisible(notification);
        }
    }

    /**
     * Check if the notification should be updated.
     *
     * @param notification
     * @return
     */
    private boolean shouldUpdateNotification(AbstractCallNotification notification) {
        if (activeNotification == null) return true;

        if (!activeNotification.getClass().equals(notification.getClass())) return true;

        if (notification.getClass().equals(ActiveCallNotification.class)) {
            notification.display();
        }

        return false;
    }

    /**
     * If the app is visible, launch the full screen intent from the activity.
     *
     * @param notification
     */
    private void launchIncomingCallActivityWhenAppIsVisible(AbstractCallNotification notification) {
        if (notification.getClass().equals(IncomingCallNotification.class)) {
            IncomingCallNotification incomingCallNotification = (IncomingCallNotification) notification;
            try {
                if (VialerApplication.get().isApplicationVisible()) {
                    incomingCallNotification.build().fullScreenIntent.send();
                }
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    public void startCallActivity(String number, @CallingConstants.CallTypes String type, Class activity) {
        mLogger.d("callVisibleForUser");
        startActivity(AbstractCallActivity.createIntentForCallActivity(this, activity));
        sipServiceActive = true;
    }

    public Session getCurrentCall() {
        return softphone.getCall();
    }

    private void startCallActivityForCurrentCall() {
        if (getCurrentCall() == null) {
            getLogger().e("Unable to start call activity for current call as there is no current call");
            return;
        }

        startOutgoingCallActivity(getCurrentCall().getPhoneNumber());
    }

    /**
     * Get the details used for a incoming call.
     * @return
     */
    public Intent getIncomingCallDetails() {
        return mIncomingCallDetails;
    }


    public Logger getLogger() {
        return mLogger;
    }

    public NativeCallManager getNativeCallManager() {
        return mNativeCallManager;
    }

    public SipConfig getSipConfig() {
        return mSipConfig;
    }

    public AbstractCallNotification getNotification() {
        return callNotification;
    }

    public void onCallConnected() {
        if (getCurrentCall() == null) return;

        if (SessionExtensionsKt.isOutgoing(getCurrentCall())) {
            mLogger.i("Call has connected, it is an outbound call so just changing audio focus");
            audioRouter.focus();
            return;
        }

        mLogger.i("Call has connected, it is an inbound call so stop all incoming call notifications and change the audio focus");

        startCallActivity(
                getCurrentCall().getPhoneNumber(),
                CallingConstants.TYPE_INCOMING_CALL,
                CallActivity.class
        );

        incomingCallAlerts.stop();
        changeNotification(callNotification.active(getCurrentCall()));
        audioRouter.focus();
    }


    public AudioRouter getAudioRouter() {
        return audioRouter;
    }

    /**
     * This method will be called every "tic"
     *
     */
    @Override
    public void onTic() {
        if (getCurrentCall() == null) return;

        Session call = getCurrentCall();

        refreshCallAlerts(call);

        if (SessionExtensionsKt.isConnected(call)) {
            audioRouter.focus();
        }
    }

    /**
     * Make sure our call alerts are correct based on the state of the call.
     *
     * @param call
     */
    private void refreshCallAlerts(Session call) {
        if (incomingAlertsMuted) {
            incomingCallAlerts.stop();
            return;
        }

        if (SessionExtensionsKt.isRinging(call)) {
            if (call.getPhoneNumber() != null && !call.getPhoneNumber().isEmpty()) {
                changeNotification(callNotification.incoming(call.getPhoneNumber(), call.getDisplayName()));
                incomingCallAlerts.start();
            }
        } else {
            incomingCallAlerts.stop();
        }
    }

    /**
     * Class the be able to bind a activity to this service.
     */
    public class SipServiceBinder extends Binder {
        public SipService getService() {
            // Return this instance of SipService so clients can call public methods.
            return SipService.this;
        }
    }

    private class PhoneStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                if (!phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    return;
                }

                // When the native call has been picked up and there is a current call in the ringing state
                // Then decline the current call.
                mLogger.i("Native call is picked up.");
                mLogger.i("Is there an active call: " + (softphone.getCall() != null));

                if (softphone.getCall() == null) {
                    return;
                }

                mLogger.i("Current call state: " + softphone.getCall().getState());

                if (SessionExtensionsKt.isRinging(softphone.getCall())) {
                    mLogger.i("Our call is still ringing. So decline it.");
                    try {
                        softphone.getPhone().declineIncoming(softphone.getCall(), Reason.BUSY);
                    } catch (SecurityException e) {
                        mLogger.e("Unable to decline incoming call due to permissions");
                    }
                    return;
                }

                if (SessionExtensionsKt.isConnected(softphone.getCall()) && !SessionExtensionsKt.isOnHold(softphone.getCall())) {
                    mLogger.i("Call was not on hold already. So put call on hold.");
                    softphone.getPhone().setHold(softphone.getCall(), true);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class OutgoingCallRinger implements Runnable {
        @Override
        public void run() {
            // Play a ring back tone to update a user that setup is ongoing.
            mToneGenerator.startTone(ToneGenerator.Constants.TONE_SUP_DIAL, 1000);
            mHandler.postDelayed(mRingbackRunnable, 4000);
        }
    }

    /**
     * We want to allow the user to press the power button to turn off
     * ringing/vibration.
     *
     */
    private class ScreenOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mLogger.i("Detected screen off event, disabling call alert");

            incomingCallAlerts.stop();
            incomingAlertsMuted = true;
        }
    }

    /**
     * Create an action for the SipService, specifying a valid action.
     *
     * @param action The action the SipService should perform when resolved
     * @return The complete pending intent
     */
    public static PendingIntent createSipServiceAction(@Actions.Valid String action) {
        return createSipServiceAction(action, null, null);
    }

    /**
     * Create an action for the SipService, specifying a valid action, the URI data and extras bundle.
     *
     * @param action The action the SipService should perform when resolved
     * @param data The URI data the intent is operating on.
     * @param bundle The extras bundle to pass additional data to the intent.
     * @return The complete pending intent
     */
    public static PendingIntent createSipServiceAction(@Actions.Valid String action, @Nullable Uri data, @Nullable Bundle bundle) {
        Intent intent = new Intent(VialerApplication.get(), SipService.class);
        intent.setAction(action);
        if (data != null) {
            intent.setData(data);
        }
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return PendingIntent.getService(VialerApplication.get(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Given a context, this will directly perform an action on the SipService.
     *
     * @param context
     * @param action
     */
    public static void performActionOnSipService(Context context, @Actions.Valid String action) {
        if (Actions.DISPLAY_CALL_IF_AVAILABLE.equals(action) && !SipService.sipServiceActive) {
            return;
        }

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * This method is called when the user opens the multi-tasking menu and swipes/closes Vialer.
     *
     * @param rootIntent
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mLogger.i("Stopping SipService as task has been removed");
        stopSelf();
    }

    /**
     * Starting this runnable spawns an ongoing check every {@value #CHECK_SERVICE_USER_INTERVAL_MS}ms that will
     * shutdown the service if there is no active call. This is a fallback to ensure the SipService is properly
     * shut down after a call if for some reason it wasn't.
     *
     */
    private class CheckServiceIsRunning implements Runnable {

        /**
         * The timeout between checks to determine the service is still running.
         *
         */
        static final int CHECK_SERVICE_USER_INTERVAL_MS = 20000;

        private void start() {
            mHandler.postDelayed(mCheckService, CHECK_SERVICE_USER_INTERVAL_MS);
        }

        @Override
        public void run() {
            stopServiceIfNoActiveCalls();
            mHandler.postDelayed(this, CHECK_SERVICE_USER_INTERVAL_MS);
        }

        private void stopServiceIfNoActiveCalls() {
            mLogger.d("checkServiceBeingUsed");
            if (softphone.getCall() == null) {
                mLogger.i("No active calls stop the service");
                stopSelf();
                if (mIncomingCallDetails != null) {
                    String number = mIncomingCallDetails.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER);
                    String contactName = mIncomingCallDetails.getStringExtra(SipConstants.EXTRA_CONTACT_NAME);
                    if (number != null && !number.isEmpty()) {
                        new MissedCallNotification(number, contactName).display();
                    }
                }
            }
        }
    }

    /**
     * This contains the list of valid actions that the SipService can
     * take. Whenever the SipService is started, the intent should contain
     * one of these.
     *
     */
    public interface Actions {


        @StringDef({HANDLE_INCOMING_CALL, HANDLE_OUTGOING_CALL, DECLINE_INCOMING_CALL, ANSWER_INCOMING_CALL, END_CALL, ANSWER_OR_HANGUP, DISPLAY_CALL_IF_AVAILABLE})
        @Retention(RetentionPolicy.SOURCE)
        @interface Valid {}

        String PREFIX = BuildConfig.APPLICATION_ID + ".";

        /**
         * An action that should be received when first creating the SipService
         * when there is an outgoing call being started.
         *
         */
        String HANDLE_OUTGOING_CALL = PREFIX + "CALL_OUTGOING";

        /**
         * An action that should be received when first creating the SipService
         * when there is an incoming call expected.
         *
         */
        String HANDLE_INCOMING_CALL = PREFIX + "CALL_INCOMING";

        /**
         * An action that should be received when there is already an active
         * call, this allows something like a notification to decline a call.
         *
         */
        String DECLINE_INCOMING_CALL = PREFIX + "DECLINE_INCOMING_CALL";

        /**
         * An action that should be received when there is already an active
         * call, this allows something like a notification to answer a call.
         *
         */
        String ANSWER_INCOMING_CALL = PREFIX + "ANSWER_INCOMING_CALL";

        /**
         * End an already in progress call.
         *
         */
        String END_CALL = PREFIX + "END_CALL";

        /**
         * This action will either answer or hangup the call based on the current call state,
         * this is to enable a user to perform multiple actions on a single button click
         * (e.g. on a bluetooth headset).
         *
         */
        String ANSWER_OR_HANGUP = PREFIX + "ANSWER_OR_HANGUP";

        /**
         * Cause the SipService to create a call activity for the current call, if there is no call
         * this action will have no affect.
         *
         */
        String DISPLAY_CALL_IF_AVAILABLE = PREFIX + "DISPLAY_CALL_IF_AVAILABLE";
    }

    private SessionCallback callback = new SessionCallback() {

        private static final String DEFAULT_EVENT = "call-update";



        private boolean canHandleIncomingCall() {
            boolean result = getCurrentCall() == null && !getNativeCallManager().isBusyWithNativeCall();

            if (!result) LogHelper.using(mLogger).logBusyReason(SipService.this);

            return result;
        }


        @Override
        public void incomingCall(@NotNull final Session incomingSession) {
            fireEvent(DEFAULT_EVENT, incomingSession);

Log.e("TEST123", "incoming call...");
            if (!canHandleIncomingCall()) {
                try {
                    softphone.getPhone().declineIncoming(incomingSession, Reason.BUSY);
                } catch (SecurityException e) {

                }
                return;
            }

            if (getCurrentCall() != null) {
                VialerStatistics.incomingCallFailedDueToOngoingVialerCall(incomingSession);
            }

            if (getNativeCallManager().isBusyWithNativeCall()) {
                VialerStatistics.incomingCallFailedDueToOngoingGsmCall(incomingSession);
            }

            softphone.setCall(incomingSession);
            informUserAboutIncomingCall(incomingSession.getPhoneNumber(), incomingSession.getDisplayName());
        }

        @Override
        public void outgoingInit(@NotNull final Session session) {
            fireEvent(DEFAULT_EVENT, session);
            softphone.setCall(session);
        }

        @Override
        public void sessionConnected(@NotNull final Session session) {
            fireEvent(CALL_CONNECTED_MESSAGE, session);
            onCallConnected();
        }

        @Override
        public void sessionEnded(@NotNull final Session session) {
            fireEvent(CALL_DISCONNECTED_MESSAGE, session);
            softphone.setCall(null);
        }

        @Override
        public void sessionReleased(@NotNull final Session session) {
            fireEvent(CALL_DISCONNECTED_MESSAGE, session);
            softphone.setCall(null);
        }

        @Override
        public void error(@NotNull final Session session) {
            fireEvent(CALL_DISCONNECTED_MESSAGE, session);
            softphone.setCall(null);
        }

        @Override
        public void sessionUpdated(@NotNull final Session session) {
            fireEvent(DEFAULT_EVENT, session);
        }

        private void fireEvent(String event, Session call) {
            Intent intent = new Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS);
            intent.putExtra(CALL_STATUS_KEY, event);
            intent.putExtra(CALL_IDENTIFIER_KEY, SessionExtensionsKt.getCallId(call));
            intent.putExtra(CALL_STATUS_CODE, call.getReason().toString());

            localBroadcastManager.sendBroadcast(intent);
        }
    };
}

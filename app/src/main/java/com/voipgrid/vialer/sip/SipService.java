package com.voipgrid.vialer.sip;

import static com.voipgrid.vialer.fcm.RemoteMessageData.MESSAGE_START_TIME;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;
import static com.voipgrid.vialer.sip.SipConstants.BUSY_TONE_DURATION;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telecom.CallAudioState;
import android.util.Log;
import android.widget.Toast;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.android.calling.AndroidCallManager;
import com.voipgrid.vialer.android.calling.AndroidCallConnection;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.bluetooth.AudioStateChangeReceiver;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts;

import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.CallStatusReceiver;
import com.voipgrid.vialer.calling.CallingConstants;
import com.voipgrid.vialer.dialer.ToneGenerator;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.notifications.call.AbstractCallNotification;
import com.voipgrid.vialer.notifications.call.ActiveCallNotification;
import com.voipgrid.vialer.notifications.call.DefaultCallNotification;
import com.voipgrid.vialer.notifications.call.IncomingCallNotification;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
public class SipService extends Service implements CallStatusReceiver.Listener {
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

    private Intent mIncomingCallDetails = null;
    private SipCall mCurrentCall;
    private SipCall mInitialCall;
    private List<SipCall> mCallList = new ArrayList<>();

    @Nullable private Intent intent;

    private Logger mLogger;
    private SipBroadcaster mSipBroadcaster;
    private Runnable mRingbackRunnable = new OutgoingCallRinger();
    private final IBinder mBinder = new SipServiceBinder();
    private CheckServiceIsRunning mCheckService = new CheckServiceIsRunning();
    private AbstractCallNotification callNotification = new DefaultCallNotification();
    private AbstractCallNotification activeNotification;
    private CallStatusReceiver callStatusReceiver = new CallStatusReceiver(this);
    private ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver();

    private AndroidCallConnection connection;

    @Inject protected SipConfig mSipConfig;
    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;
    @Inject protected Handler mHandler;
    @Inject protected ToneGenerator mToneGenerator;
    @Inject protected NetworkConnectivity mNetworkConnectivity;
    @Inject protected NativeCallManager mNativeCallManager;
    @Inject @Nullable protected PhoneAccount mPhoneAccount;
    @Inject IncomingCallAlerts incomingCallAlerts;
    @Inject AndroidCallManager androidCallManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mLogger = new Logger(SipService.class);
        mSipBroadcaster = new SipBroadcaster(this);
        VialerApplication.get().component().inject(this);
        AudioStateChangeReceiver.fetch();
        mLogger.d("onCreate");

        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mNetworkConnectivity, ConnectivityManager.CONNECTIVITY_ACTION);
        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(callStatusReceiver, ACTION_BROADCAST_CALL_STATUS);
        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(screenOffReceiver, Integer.MAX_VALUE, Intent.ACTION_SCREEN_OFF);
        mCheckService.start();
        changeNotification(callNotification);
        AndroidCallConnection.voip = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLogger.i("mSipServiceHasHandledACall: " + mSipServiceHasHandledACall);

        // If the SipService has already handled a call but now has no call, this suggests
        // that the SipService is stuck not doing anything so it should be immediately shut
        // down.
        if (mSipServiceHasHandledACall && mCurrentCall == null) {
            mLogger.i("onStartCommand was triggered after a call has already been handled but with no current call, stopping SipService...");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            boolean shouldStartForeground = performActionBasedOnIntent(intent);

            if (shouldStartForeground) {
                startForeground(callNotification.getNotificationId(), callNotification.build());
            }
        } catch (Exception e) {
            mLogger.e("Failed to perform action based on intent, stopping service: " + e.getMessage());
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
    public boolean performActionBasedOnIntent(Intent intent) throws Exception {
        if (intent == null) return false;

        this.intent = intent;
        final String action = this.intent.getAction();

        mLogger.i("Performing action: " + action);

        if (Actions.HANDLE_INCOMING_CALL.equals(action)) {
            initialiseIncomingCall();
            return true;
        }
        else if (Actions.HANDLE_OUTGOING_CALL.equals(action)) {
            initialiseOutgoingCall();
            return true;
        }
        else if (Actions.DECLINE_INCOMING_CALL.equals(action)){
            connection.onReject();
        }
        else if (Actions.ANSWER_INCOMING_CALL.equals(action)) {
            if (!MicrophonePermission.hasPermission(VialerApplication.get())) {
                Toast.makeText(this, getString(R.string.permission_microphone_missing_message), Toast.LENGTH_LONG).show();
                mLogger.e("Unable to answer incoming call as we do not have microphone permission");
                return false;
            }

            connection.onAnswer();
        }
        else if (Actions.END_CALL.equals(action)) {
            connection.onDisconnect();
        }
        else if (Actions.DISPLAY_CALL_IF_AVAILABLE.equals(action)) {
            if (getCurrentCall() != null) {
                if (getCurrentCall().isConnected()) {
                    startCallActivityForCurrentCall();
                }
            } else {
                stopSelf();
            }
        }
        else {
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
    }

    /**
     * Display the incoming call to the user.
     *
     */
    public void showIncomingCallToUser() {
        incomingCallAlerts.start();
        changeNotification(callNotification.incoming(getCurrentCall().getPhoneNumber(), getCurrentCall().getCallerId()));
    }

    /**
     * Perform necessary steps to initialise an outgoing call.
     *
     */
    private void initialiseOutgoingCall() {
        if (getCurrentCall() != null) {
            getLogger().i("Attempting to initialise a second outgoing call but this is not currently supported");
            startCallActivityForCurrentCall();
            return;
        }

        this.androidCallManager.call(this.intent.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER));
    }

    /**
     * A callback when the android call manager is prepared
     *
     */
    public void androidCallManagerIsReadyForOutgoingCall() {
        loadSip();
        makeCall(
                this.intent.getData(),
                this.intent.getStringExtra(SipConstants.EXTRA_CONTACT_NAME),
                this.intent.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER),
                true
        );
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
            mSipConfig.initLibrary();
        } catch (Exception e) {
            mLogger.e("Failed to load pjsip, stopping the service");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mLogger.d("onDestroy");

        if (connection != null) {
            connection.destroy();
        }
        silence();

        if (mSipConfig != null) {
            mSipConfig.cleanUp();
        }

        mSipBroadcaster.broadcastServiceInfo(SipConstants.SERVICE_STOPPED);

        mBroadcastReceiverManager.unregisterReceiver(mNetworkConnectivity, callStatusReceiver, screenOffReceiver);

        mHandler.removeCallbacks(mCheckService);

        sipServiceActive = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Play the busy tone used when a call get's disconnected by the recipient.
     */
    public void playBusyTone() {
        try {
            mToneGenerator.startTone(ToneGenerator.Constants.TONE_CDMA_NETWORK_BUSY, BUSY_TONE_DURATION);
            Thread.sleep(BUSY_TONE_DURATION);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Start the ring back for a outgoing call.
     */
    public void startRingback() {
        mLogger.d("onCallStartRingback");
        mHandler.postDelayed(mRingbackRunnable, 2000);
    }

    /**
     * Stop the ring back for a outgoing call.
     */
    public void stopRingback() {
        mLogger.d("onCallStopRingback");
        mHandler.removeCallbacks(mRingbackRunnable);
    }

    /**
     * Function to make a outgoing call without starting a activity.
     * @param number
     * @param contactName
     * @param phoneNumber
     */
    public void makeCall(Uri number, String contactName, String phoneNumber) {
        makeCall(number, contactName, phoneNumber, false);
    }

    /**
     * Function to make a call with or without starting a activity.
     * @param number
     * @param contactName
     * @param phoneNumber
     * @param startActivity
     */
    public void makeCall(Uri number, String contactName, String phoneNumber, boolean startActivity) {
        SipCall call = new SipCall(this, getSipConfig().getSipAccount());
        call.setPhoneNumberUri(number);
        call.setCallerId(contactName);
        call.setPhoneNumber(phoneNumber);

        boolean success = call.startOutgoingCall(number);

        if (success && startActivity) {
            startOutgoingCallActivity(call, call.getPhoneNumberUri());
        }
    }

    /**
     * Start the activity for a (initial) outgoing call.
     * @param sipCall
     * @param number
     */
    public void startOutgoingCallActivity(SipCall sipCall, Uri number) {
        startCallActivity(
                number,
                CallingConstants.TYPE_OUTGOING_CALL,
                sipCall.getCallerId(),
                sipCall.getPhoneNumber(),
                CallActivity.class
        );

        changeNotification(callNotification.outgoing(sipCall));
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

    public void startCallActivity(Uri sipAddressUri, @CallingConstants.CallTypes String type, String callerId, String number, Class activity) {
        mLogger.d("callVisibleForUser");
        Log.e("TEST123", "Starting call activity");
        startActivity(AbstractCallActivity.createIntentForCallActivity(
                this,
                activity,
                sipAddressUri,
                type,
                callerId,
                number
        ));
        sipServiceActive = true;
    }

    /**
     * Set the current call and add it to the list of calls if it does not exists. If this is the
     * first call made set mInitialCall as well.
     * @param call
     */
    public void setCurrentCall(SipCall call) {
        mSipServiceHasHandledACall = true;
        if (call != null && mInitialCall == null) {
            setInitialCall(call);
        }
        mCurrentCall = call;
        if (!mCallList.contains(call) && call != null) {
            mCallList.add(call);
        }
    }

    public SipCall getCurrentCall() {
        return mCurrentCall;
    }

    /**
     * Removes the call from the list and deletes it. If there are no calls left stop
     * the service.
     * @param call
     */
    public void removeCallFromList(SipCall call) {
        mCallList.remove(call);

        if (mCallList.isEmpty() || call.getCallIsTransferred()) {
            setCurrentCall(null);
            stopSelf();
        } else {
            setCurrentCall(mCallList.get(0));
        }
    }

    private void startCallActivityForCurrentCall() {
        if (getCurrentCall() == null) {
            getLogger().e("Unable to start call activity for current call as there is no current call");
            return;
        }

        startOutgoingCallActivity(getCurrentCall(), getCurrentCall().getPhoneNumberUri());
    }

    /**
     * Get the details used for a incoming call.
     * @return
     */
    public Intent getIncomingCallDetails() {
        return mIncomingCallDetails;
    }

    private void setInitialCall(SipCall initialCall) {
        mInitialCall = initialCall;
    }

    public SipCall getInitialCall() {
        return mInitialCall;
    }

    public SipCall getFirstCall() {

        if (mCallList.size() > 0) {
            return mCallList.get(0);
        } else {
            return null;
        }
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

    public SipBroadcaster getSipBroadcaster() {
        return mSipBroadcaster;
    }

    public AbstractCallNotification getNotification() {
        return callNotification;
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {

    }

    @Override
    public void onCallConnected() {
        if (getCurrentCall() == null) return;

        if (getCurrentCall().isOutgoing()) {
            connection.setActive();
            return;
        }

        mLogger.i("Call has connected, it is an inbound call so stop all incoming call notifications");

        startCallActivity(
                SipUri.sipAddressUri(this, PhoneNumberUtils.format(getCurrentCall().getPhoneNumber())),
                CallingConstants.TYPE_INCOMING_CALL,
                getCurrentCall().getCallerId(),
                getCurrentCall().getPhoneNumber(),
                CallActivity.class
        );

        silence();
        changeNotification(callNotification.active(getCurrentCall()));
    }

    @Override
    public void onCallDisconnected() {
        Log.e("TEST123", "onDisconnected");

    }

    @Override
    public void onCallHold() {

    }

    @Override
    public void onCallUnhold() {

    }

    @Override
    public void onCallRingingOut() {

    }

    @Override
    public void onCallRingingIn() {

    }

    @Override
    public void onServiceStopped() {

    }

    public void silence() {
        incomingCallAlerts.stop();
    }

    public AndroidCallConnection getConnection() {
        return connection;
    }

    public void onIncomingCall(final OnIncomingCallParam incomingCallParam, SipAccount account) {
        SipCall sipCall = new SipCall(this, account, incomingCallParam.getCallId(), new SipInvite(incomingCallParam.getRdata().getWholeMsg()));
        sipCall.onCallIncoming();

        if (getIncomingCallDetails() != null) {
            Intent incomingCallDetails = getIncomingCallDetails();
            sipCall.setMiddlewareKey(incomingCallDetails.getStringExtra(SipConstants.EXTRA_REQUEST_TOKEN));
            sipCall.setMessageStartTime(incomingCallDetails.getStringExtra(MESSAGE_START_TIME));

        }

        androidCallManager.incomingCall();
    }

    public SipAccount createSipAccount(final AccountConfig accountConfig) throws Exception {
        return new SipAccount(accountConfig);
    }

    /**
     * Android has reported that the call audio state has changed.
     *
     * @param state
     */
    public void onCallAudioStateChanged(final CallAudioState state) {
        Log.e("TEST123", state.toString());
        Toast.makeText(VialerApplication.get(), state.toString(), Toast.LENGTH_LONG).show();
        sendBroadcast(new Intent("VialerConnection"));
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
//            mLogger.i("Detected screen off event, disabling call alert");
//
//            incomingCallAlerts.stop();
//            incomingAlertsMuted = true;
        }
    }

    /**
     * Create an action for the SipService, specifying a valid action.
     *
     * @param action The action the SipService should perform when resolved
     * @return The complete pending intent
     */
    public static PendingIntent createSipServiceAction(@Actions.Valid String action) {
        Intent intent = new Intent(VialerApplication.get(), SipService.class);
        intent.setAction(action);
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

    public void setConnection(AndroidCallConnection connection) {
        this.connection = connection;
        Log.e("TEST123", "Set a connection!!!");
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
            if (mCurrentCall == null) {
                mLogger.i("No active calls stop the service");
                stopSelf();
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
        @StringDef({HANDLE_INCOMING_CALL, HANDLE_OUTGOING_CALL, DECLINE_INCOMING_CALL, ANSWER_INCOMING_CALL, END_CALL, DISPLAY_CALL_IF_AVAILABLE})
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
         * Cause the SipService to create a call activity for the current call, if there is no call
         * this action will have no affect.
         *
         */
        String DISPLAY_CALL_IF_AVAILABLE = PREFIX + "DISPLAY_CALL_IF_AVAILABLE";
    }

    class SipAccount extends org.pjsip.pjsua2.Account {

        private SipAccount(AccountConfig accountConfig) throws Exception {
            super();
            create(accountConfig);
        }

        @Override
        public void onIncomingCall(OnIncomingCallParam incomingCallParam) {
            SipService.this.onIncomingCall(incomingCallParam, this);
        }


        @Override
        public void onRegState(OnRegStateParam regStateParam) {
            try {
                if (getInfo().getRegIsActive()) {
                    getSipConfig().onAccountRegistered(this, regStateParam);
                }
            } catch (Exception exception) {
            }
        }
    }

}

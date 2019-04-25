package com.voipgrid.vialer.sip;

import static com.voipgrid.vialer.sip.SipConstants.ACTION_CALL_INCOMING;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_CALL_OUTGOING;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.bluetooth.AudioStateChangeReceiver;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.calling.CallingConstants;
import com.voipgrid.vialer.calling.IncomingCallActivity;
import com.voipgrid.vialer.dialer.ToneGenerator;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
public class SipService extends Service {

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
    private String mInitialCallType;

    @Nullable private Intent mIntent;

    private Logger mLogger;
    private SipBroadcaster mSipBroadcaster;
    private final BroadcastReceiver phoneStateReceiver = new PhoneStateReceiver();
    private Runnable mRingbackRunnable = new OutgoingCallRinger();
    private final IBinder mBinder = new SipServiceBinder();
    private CheckServiceIsRunning mCheckService = new CheckServiceIsRunning();
    @Inject protected SipConfig mSipConfig;
    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;
    @Inject protected Handler mHandler;
    @Inject protected Preferences mPreferences;
    @Inject protected ToneGenerator mToneGenerator;
    @Inject protected NetworkConnectivity mNetworkConnectivity;
    @Inject protected NativeCallManager mNativeCallManager;
    @Inject @Nullable protected PhoneAccount mPhoneAccount;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLogger = new Logger(SipService.class);
        mSipBroadcaster = new SipBroadcaster(this);
        VialerApplication.get().component().inject(this);
        AudioStateChangeReceiver.fetch();
        mLogger.d("onCreate");

        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(phoneStateReceiver, TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mNetworkConnectivity, ConnectivityManager.CONNECTIVITY_ACTION);
        mCheckService.start();
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

        handleCallDeclineIntentIfAppropriate();

        initialiseCallBasedOnIntent(intent);

        return START_NOT_STICKY;
    }

    /**
     * Initialise the call based on the information in the intent.
     *
     * @param intent
     */
    public void initialiseCallBasedOnIntent(Intent intent) {
        if (intent == null) return;

        mIntent = intent;
        mInitialCallType = mIntent.getAction();

        if (ACTION_CALL_INCOMING.equals(mInitialCallType)) {
            initialiseIncomingCall();
        }
        else if (ACTION_CALL_OUTGOING.equals(mInitialCallType)) {
            initialiseOutgoingCall(intent);
        }
        else {
            mLogger.e("Stopping SipService as an intent with no action was received");
            stopSelf();
        }
    }

    /**
     * Perform necessary steps to initialise an incoming call.
     *
     */
    private void initialiseIncomingCall() {
        mLogger.d("incomingCall");
        mIncomingCallDetails = mIntent;
        loadSip();
    }

    /**
     * Perform necessary steps to initialise an outgoing call.
     *
     */
    private void initialiseOutgoingCall(Intent intent) {
        mLogger.d("outgoingCall");
        loadSip();
        makeCall(
                intent.getData(),
                mIntent.getStringExtra(SipConstants.EXTRA_CONTACT_NAME),
                mIntent.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER),
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
            mSipConfig = mSipConfig.init(this, mPhoneAccount);
            mSipConfig.initLibrary();
        } catch (Exception e) {
            mLogger.e("Failed to load pjsip, stopping the service");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mLogger.d("onDestroy");

        // If no phoneaccount was found in the onCreate there won't be a sipconfig either.
        // Check to avoid nullpointers.
        if (mSipConfig != null) {
            mSipConfig.cleanUp();
        }

        mSipBroadcaster.broadcastServiceInfo(SipConstants.SERVICE_STOPPED);

        try {
            mBroadcastReceiverManager.unregisterReceiver(phoneStateReceiver, mNetworkConnectivity);
        } catch(IllegalArgumentException e) {
            mLogger.w("Trying to unregister phoneStateReceiver not registered.");
        }

        mHandler.removeCallbacks(mCheckService);

        sipServiceActive = false;
        super.onDestroy();
    }

    /**
     * We will also handle an intent that is telling us to decline the incoming call,
     * this intent is currently sent when the user presses decline on the call
     * notification as there is no activity to defer to, it must be handled here.
     *
     */
    private void handleCallDeclineIntentIfAppropriate() {
        if (mIntent == null || mIntent.getType() == null) return;

        if (!mIntent.getType().equals(SipConstants.CALL_DECLINE_INCOMING_CALL)) return;

        try {
            mLogger.i("Attempting to decline a call based on the intent type broadcast to the SipService");
            mCurrentCall.decline();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            stopSelf();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Play the busy tone used when a call get's disconnected by the recipient.
     */
    public void playBusyTone() {
        mToneGenerator.startTone(ToneGenerator.Constants.TONE_CDMA_NETWORK_BUSY, 1500);
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
        call.onCallOutgoing(number, startActivity);
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
    }

    /**
     * Start the activity for a incoming call.
     * @param number
     * @param callerId
     */
    public void startIncomingCallActivity(String number, String callerId) {
        startCallActivity(
                SipUri.sipAddressUri(this, PhoneNumberUtils.format(number)),
                CallingConstants.TYPE_INCOMING_CALL,
                callerId,
                number,
                IncomingCallActivity.class
        );
    }

    private void startCallActivity(Uri sipAddressUri, @CallingConstants.CallTypes String type, String callerId, String number, Class activity) {
        mLogger.d("callVisibleForUser");
        Intent intent = new Intent(this, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setDataAndType(sipAddressUri, type);
        intent.putExtra(CallingConstants.CONTACT_NAME, callerId);
        intent.putExtra(CallingConstants.PHONE_NUMBER, number);

        sipServiceActive = true;
        startActivity(intent);
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

        if (mCallList.isEmpty()) {
            setCurrentCall(null);
            NotificationHelper notificationHelper = NotificationHelper.getInstance(this);
            notificationHelper.removeAllNotifications();
            stopSelf();
        } else if (call.getCallIsTransferred()) {
            setCurrentCall(null);
        } else {
            setCurrentCall(mCallList.get(0));
        }
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

    public Preferences getPreferences() {
        return mPreferences;
    }

    public NativeCallManager getNativeCallManager() {
        return mNativeCallManager;
    }

    public String getInitialCallType() {
        return mInitialCallType;
    }

    public SipConfig getSipConfig() {
        return mSipConfig;
    }

    public SipBroadcaster getSipBroadcaster() {
        return mSipBroadcaster;
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
                mLogger.e("Native call is picked up.");
                mLogger.e("Is there an active call: " + (mCurrentCall != null));

                if (mCurrentCall == null) {
                    return;
                }

                mLogger.e("Current call state: " + mCurrentCall.getCurrentCallState());

                if (mCurrentCall.isCallRinging() || mCurrentCall.getCurrentCallState().equals(SipConstants.CALL_INVALID_STATE)) {
                    mLogger.e("Our call is still ringing. So decline it.");
                    mCurrentCall.decline();
                    return;
                }

                if (mCurrentCall.isConnected() && !mCurrentCall.isOnHold()) {
                    mLogger.e("Call was not on hold already. So put call on hold.");
                    mCurrentCall.toggleHold();
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
}

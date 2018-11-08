package com.voipgrid.vialer.sip;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

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
public class SipService extends Service implements SipConfig.Listener {
    private final IBinder mBinder = new SipServiceBinder();

    private Handler mHandler;
    private Intent mIncomingCallDetails = null;
    private ToneGenerator mToneGenerator;

    private Preferences mPreferences;
    private Logger mLogger;
    private SipBroadcaster mSipBroadcaster;
    private SipCall mCurrentCall;
    private SipCall mInitialCall;
    private NativeCallManager mNativeCallManager;

    private List<SipCall> mCallList = new ArrayList<>();
    private String mInitialCallType;

    private static final int CHECK_SERVICE_USER_INTERVAL_MS = 20000;
    private Handler mCheckServiceHandler;
    private Runnable mCheckServiceRunnable;
    @Nullable private Intent mIntent;

    @Inject SipConfig mSipConfig;

    /**
     * This will track whether this instance of SipService has ever handled a call,
     * if this is the case we can shut down the sip service immediately if we don't
     * have a call when onStartCommand is run.
     */
    private boolean mSipServiceHasHandledACall = false;

    private final BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
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
    };

    /**
     * Set when the SipService is active. This is used to respond to the middleware.
     */
    public static boolean sipServiceActive = false;

    /**
     * Class the be able to bind a activity to this service.
     */
    public class SipServiceBinder extends Binder {
        public SipService getService() {
            // Return this instance of SipService so clients can call public methods.
            return SipService.this;
        }
    }

    /**
     * SIP does not present Media by default.
     * Use Android's ToneGenerator to play a dial tone at certain required times.
     * @see for usage of delayed "mRingbackRunnable" callback.
     */
    private Runnable mRingbackRunnable = new Runnable() {
        @Override
        public void run() {
            // Play a ring back tone to update a user that setup is ongoing.
            mToneGenerator.startTone(ToneGenerator.Constants.TONE_SUP_DIAL, 1000);
            mHandler.postDelayed(mRingbackRunnable, 4000);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VialerApplication.get().component().inject(this);
        AudioStateChangeReceiver.fetch();

        mHandler = new Handler();

        mToneGenerator = new ToneGenerator(
                AudioManager.STREAM_VOICE_CALL,
                SipConstants.RINGING_VOLUME);

        mSipBroadcaster = new SipBroadcaster(this);

        mPreferences = new Preferences(this);
        mLogger = new Logger(SipService.class);
        mNativeCallManager = new NativeCallManager((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));

        mLogger.d("onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

        registerReceiver(phoneStateReceiver, filter);

        // Create runnable to check if the SipService is still in use.
        mCheckServiceHandler = new Handler();
        mCheckServiceRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if the service is being used after 10 seconds and shutdown the service
                // if required.
                checkServiceBeingUsed();
                mCheckServiceHandler.postDelayed(this, CHECK_SERVICE_USER_INTERVAL_MS);
            }
        };
        mCheckServiceHandler.postDelayed(mCheckServiceRunnable, CHECK_SERVICE_USER_INTERVAL_MS);

        PhoneAccount phoneAccount = new JsonStorage<PhoneAccount>(this).get(PhoneAccount.class);
        if (phoneAccount != null) {
            // Try to load PJSIP library.
            mSipConfig = mSipConfig.init(this, phoneAccount);
            mSipConfig.initLibrary(this);
        } else {
            // User has no sip account so destroy the service.
            mLogger.w("No sip account when trying to create service");
            stopSelf();
        }
    }

    private void checkServiceBeingUsed() {
        mLogger.d("checkServiceBeingUsed");
        if (mCurrentCall == null) {
            mLogger.i("No active calls stop the service");
            stopSelf();
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
            unregisterReceiver(phoneStateReceiver);
        } catch(IllegalArgumentException e) {
            mLogger.w("Trying to unregister phoneStateReceiver not registered.");
        }

        mCheckServiceHandler.removeCallbacks(mCheckServiceRunnable);

        sipServiceActive = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLogger.d("onStartCommand");
        mLogger.i("onStartCommand mSipServiceHasHandledACall: " + mSipServiceHasHandledACall);

        // If the SipService has already handled a call but now has no call, this suggests
        // that the SipService is stuck not doing anything so it should be immediately shut
        // down.
        if (mSipServiceHasHandledACall && mCurrentCall == null) {
            mLogger.i("onStartCommand was triggered after a call has already been handled but with no current call, stopping SipService...");
            stopSelf();
            return START_NOT_STICKY;
        }

        mIntent = intent;

        return START_NOT_STICKY;
    }

    @Override
    public void pjSipDidLoad() {
        if (mIntent == null) {
            return;
        }

        mInitialCallType = mIntent.getAction();
        Uri number = mIntent.getData();

        switch (mInitialCallType) {
            case SipConstants.ACTION_CALL_INCOMING:
                mLogger.d("incomingCall");
                mIncomingCallDetails = mIntent;
                break;
            case SipConstants.ACTION_CALL_OUTGOING:
                mLogger.d("outgoingCall");
                makeCall(
                        number,
                        mIntent.getStringExtra(SipConstants.EXTRA_CONTACT_NAME),
                        mIntent.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER),
                        true
                );
                break;
            default:
                stopSelf();
        }

        if (mIntent.getType() != null) {
            if (mIntent.getType().equals(SipConstants.CALL_DECLINE_INCOMING_CALL)) {
                try {
                    mCurrentCall.decline();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    stopSelf();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void pjSipFailedToLoad(Exception e) {
        mLogger.e("Unable to load pjsip: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        stopSelf();
    }

    public SipBroadcaster getSipBroadcaster() {
        return mSipBroadcaster;
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
        new Thread(() -> {
            SipCall call = new SipCall(this, getSipConfig().getSipAccount());
            call.setPhoneNumberUri(number);
            call.setCallerId(contactName);
            call.setPhoneNumber(phoneNumber);
            call.onCallOutgoing(number, startActivity);
        }).start();
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
}

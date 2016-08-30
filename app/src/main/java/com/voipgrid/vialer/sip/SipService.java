package com.voipgrid.vialer.sip;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.util.GsmCallListener;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.PhonePermission;
import com.voipgrid.vialer.logging.RemoteLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * SipService ensures proper lifecycle management for the PJSUA2 library and
 * provides a persistent interface to SIP services throughout the app.
 *
 */
public class SipService extends Service {
    private final static String TAG = SipService.class.getSimpleName(); // TAG used for debug Logs
    private final IBinder mBinder = new SipServiceBinder();

    private Handler mHandler;
    private Intent mIncomingCallDetails = null;
    private ToneGenerator mToneGenerator;

    private GsmCallListener mGsmCallListener;
    private Preferences mPreferences;
    private RemoteLogger mRemoteLogger;
    private SipBroadcaster mSipBroadcaster;
    private SipCall mCurrentCall;
    private SipCall mInitialCall;
    private SipConfig mSipConfig;

    private List<SipCall> mCallList = new ArrayList<>();
    private String mInitialCallType;

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
     * @see # for usage of delayed "mRingbackRunnable" callback.
     */
    private Runnable mRingbackRunnable = new Runnable() {
        @Override
        public void run() {
            // Play a ring back tone to update a user that setup is ongoing.
            mToneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL, 1000);
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

        mHandler = new Handler();

        mToneGenerator = new ToneGenerator(
                AudioManager.STREAM_VOICE_CALL,
                SipConstants.RINGING_VOLUME);

        mSipBroadcaster = new SipBroadcaster(this);

        mPreferences = new Preferences(this);
        mRemoteLogger = new RemoteLogger(this);

        mRemoteLogger.d(TAG + " onCreate");

        PhoneAccount phoneAccount = new JsonStorage<PhoneAccount>(this).get(PhoneAccount.class);
        if (phoneAccount != null) {
            // Try to load PJSIP library.
            mSipConfig = new SipConfig(this, phoneAccount);
            try {
                mSipConfig.initLibrary();
            } catch (SipConfig.LibraryInitFailedException e) {
                stopSelf();
            }
        } else {
            // User has no sip account so destroy the service.
            mRemoteLogger.w("No sip account when trying to create service");
            stopSelf();
        }
    }

    public RemoteLogger getRemoteLogger() {
        return mRemoteLogger;
    }

    public Preferences getPreferences() {
        return mPreferences;
    }

    public String getInitialCallType() {
        return mInitialCallType;
    }

    public SipConfig getSipConfig() {
        return mSipConfig;
    }

    @Override
    public void onDestroy() {
        mRemoteLogger.d(TAG + " onDestroy");
        stopGsmCallListener();

        mSipConfig.cleanUp();

        mSipBroadcaster.broadcastServiceInfo(SipConstants.SERVICE_STOPPED);

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRemoteLogger.d(TAG + " onStartCommand");
        mInitialCallType = intent.getAction();
        Uri number = intent.getData();
        switch (mInitialCallType) {
            case SipConstants.ACTION_VIALER_INCOMING :
                mRemoteLogger.d(TAG + " incomingCall");
                mIncomingCallDetails = intent;
                break;
            case SipConstants.ACTION_VIALER_OUTGOING :
                mRemoteLogger.d(TAG + " outgoingCall");
                makeCall(
                        number,
                        intent.getStringExtra(SipConstants.EXTRA_CONTACT_NAME),
                        intent.getStringExtra(SipConstants.EXTRA_PHONE_NUMBER),
                        true
                );
                break;
            default:
                stopSelf();
        }
        return START_NOT_STICKY;
    }

    public SipBroadcaster getSipBroadcaster() {
        return mSipBroadcaster;
    }

    public void startGsmCallListener() {
        if (PhonePermission.hasPermission(getApplicationContext())) {
            if (mGsmCallListener == null) {
                mGsmCallListener = new GsmCallListener(mCallList);
                registerReceiver(mGsmCallListener, new IntentFilter("android.intent.action.PHONE_STATE"));
            }
        }
    }

    public void stopGsmCallListener() {
        if (PhonePermission.hasPermission(getApplicationContext())) {
            if (mGsmCallListener != null) {
                unregisterReceiver(mGsmCallListener);
            }
        }
    }

    /**
     * Play the busy tone used when a call get's disconnected by the recipient.
     */
    public void playBusyTone() {
        mToneGenerator.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 1500);
    }

    /**
     * Start the ring back for a outgoing call.
     */
    public void startRingback() {
        mRemoteLogger.d(TAG + " onCallStartRingback");
        mHandler.postDelayed(mRingbackRunnable, 2000);
    }

    /**
     * Stop the ring back for a outgoing call.
     */
    public void stopRingback() {
        mRemoteLogger.d(TAG + " onCallStopRingback");
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
        mRemoteLogger.d(TAG + " callVisibleForUser");
        Intent intent = new Intent(this, CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(number, CallActivity.TYPE_OUTGOING_CALL);
        intent.putExtra(CallActivity.CONTACT_NAME, sipCall.getCallerId());
        intent.putExtra(CallActivity.PHONE_NUMBER, sipCall.getPhoneNumber());

        startActivity(intent);
    }

    /**
     * Start the activity for a incoming call.
     * @param number
     * @param callerId
     */
    public void startIncomingCallActivity(String number, String callerId) {
        mRemoteLogger.d(TAG + " callVisibleForUser");
        Intent intent = new Intent(this, CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri sipAddressUri = SipUri.sipAddressUri(
                this,
                PhoneNumberUtils.format(number)
        );
        intent.setDataAndType(sipAddressUri, CallActivity.TYPE_INCOMING_CALL);
        intent.putExtra(CallActivity.CONTACT_NAME, callerId);
        intent.putExtra(CallActivity.PHONE_NUMBER, number);
        startActivity(intent);
    }

    /**
     * Set the current call and add it to the list of calls if it does not exists. If this is the
     * first call made set mInitialCall as well.
     * @param call
     */
    public void setCurrentCall(SipCall call) {
        if (call != null && mInitialCall == null) {
            setInitialCall(call);
        }
        mCurrentCall = call;
        if (!mCallList.contains(call) && call != null) {
            mCallList.add(call);
            // Update the GsmCallListener with the updated call list.
            if (mGsmCallListener != null) {
                mGsmCallListener.updateSipCallsList(mCallList);
            }
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

        // Update the GsmCallListener with the updated call list.
        if (mGsmCallListener != null) {
            mGsmCallListener.updateSipCallsList(mCallList);
        }

        if (mCallList.isEmpty()) {
            setCurrentCall(null);
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

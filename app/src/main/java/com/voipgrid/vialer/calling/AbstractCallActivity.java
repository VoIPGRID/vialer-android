package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.CALL_BTN;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.media.MediaManager;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.ProximitySensorHelper;

import javax.inject.Inject;

public abstract class AbstractCallActivity extends LoginRequiredActivity implements
        SipServiceConnection.SipServiceConnectionListener, CallDurationTracker.Listener, BluetoothButtonReceiver.Listener, CallStatusReceiver.Listener,
        MediaManager.AudioChangedInterface {

    protected SipServiceConnection mSipServiceConnection;
    protected String mCurrentCallId;
    protected CallDurationTracker mCallDurationTracker;
    protected BluetoothButtonReceiver mBluetoothButtonReceiver;
    protected CallStatusReceiver mCallStatusReceiver;
    protected DelayedFinish mDelayedFinish;

    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;
    @Inject protected CallNotifications mCallNotifications;

    protected boolean mBluetoothDeviceConnected = false;
    protected boolean mBluetoothAudioActive;
    private ProximitySensorHelper mProximityHelper;
    protected MediaManager mMediaManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VialerApplication.get().component().inject(this);
        mSipServiceConnection = new SipServiceConnection(this);
        mCallDurationTracker = new CallDurationTracker(mSipServiceConnection);
        mBluetoothButtonReceiver = new BluetoothButtonReceiver(this);
        mCallStatusReceiver = new CallStatusReceiver(this);
        mDelayedFinish = new DelayedFinish(this, new Handler(), mSipServiceConnection);
        mProximityHelper = new ProximitySensorHelper(this);
        mMediaManager = MediaManager.init(this, this, this);

        requestMicrophonePermissionIfNecessary();
        configureActivityFlags();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(mCallStatusReceiver, ACTION_BROADCAST_CALL_STATUS);
        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mBluetoothButtonReceiver, CALL_BTN, DECLINE_BTN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSipServiceConnection.connect();
        mCallDurationTracker.start(this);
        mProximityHelper.startSensor(findViewById(R.id.screen_off));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSipServiceConnection.disconnect();
        mProximityHelper.stopSensor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBroadcastReceiverManager.unregisterReceiver(mCallStatusReceiver, mBluetoothButtonReceiver);
        mMediaManager.deInit();
    }

    @Override
    @CallSuper
    public void sipServiceHasConnected(SipService sipService) {
        if (sipService.getFirstCall() != null) {
            mCurrentCallId = sipService.getFirstCall().getIdentifier();
        }
    }

    @Override
    @CallSuper
    public void sipServiceBindingFailed() {}

    @Override
    @CallSuper
    public void sipServiceHasBeenDisconnected() {}

    @Override
    public void onCallDurationUpdate(long duration) {}

    private void requestMicrophonePermissionIfNecessary() {
        if (!MicrophonePermission.hasPermission(this)) {
            MicrophonePermission.askForPermission(this);
        }
    }

    private void configureActivityFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    public void onCallStatusReceived(String status, String callId) {}

    /**
     * Attempt to shutdown the activity after a few seconds giving the services enough
     * time to clean up.
     *
     */
    protected void finishAfterDelay() {
        mDelayedFinish.begin();
    }

    @Override
    public void bluetoothDeviceConnected(boolean connected) {
        mLogger.i("BluetoothDeviceConnected()");
        mLogger.i("==>" + connected);
        mBluetoothDeviceConnected = connected;
    }

    @Override
    public void bluetoothAudioAvailable(boolean available) {
        mLogger.i("BluetoothAudioAvailable()");
        mLogger.i("==> " + available);
        mBluetoothAudioActive = available;
    }

    @Override
    public void audioLost(boolean lost) {
        mLogger.i("AudioLost or Recovered: ");
        mLogger.i("==> " + lost);
    }
}

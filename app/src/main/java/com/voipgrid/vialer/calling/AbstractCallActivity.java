package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.CALL_BTN;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.WindowManager;
import android.widget.TextView;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.media.MediaManager;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.ProximitySensorHelper;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.Optional;

public abstract class AbstractCallActivity extends LoginRequiredActivity implements
        SipServiceConnection.SipServiceConnectionListener, CallDurationTracker.Listener, BluetoothButtonReceiver.Listener, CallStatusReceiver.Listener,
        MediaManager.AudioChangedInterface {

    protected SipServiceConnection mSipServiceConnection;
    protected String mCurrentCallId;
    protected CallDurationTracker mCallDurationTracker;
    protected BluetoothButtonReceiver mBluetoothButtonReceiver;
    protected CallStatusReceiver mCallStatusReceiver;
    protected DelayedFinish mDelayedFinish;

    @Nullable @BindView(R.id.duration_text_view) TextView mCallDurationView;

    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;
    @Inject protected CallNotifications mCallNotifications;

    protected boolean mBluetoothDeviceConnected = false;
    protected boolean mBluetoothAudioActive;
    private ProximitySensorHelper mProximityHelper;

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

        requestMicrophonePermissionIfNecessary();
        configureActivityFlags();
        getMediaManager().callStarted();
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
        getMediaManager().deInit();
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

    @Override
    public void bluetoothCallButtonWasPressed() {
        onPickupButtonClicked();
    }

    @Override
    public void bluetoothDeclineButtonWasPressed() {
        onDeclineButtonClicked();
    }

    protected void onPickupButtonClicked() {

    }

    protected void onDeclineButtonClicked() {

    }

    @Override
    public void finish() {
        if (isTaskRoot() && VialerApplication.get().isApplicationVisible()) {
            mLogger.i("There are no more activities, to counter an loop of starting CallActivity, start the MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        super.finish();
    }

    protected String getPhoneNumberFromIntent() {
        return getIntent().getStringExtra(PHONE_NUMBER);
    }

    protected String getCallerIdFromIntent() {
        return getIntent().getStringExtra(CONTACT_NAME);
    }

    @Optional
    public void onCallDurationUpdate(long seconds) {
        if (!mSipServiceConnection.get().getCurrentCall().isConnected()) {
            return;
        }

        if (mCallDurationView == null) {
            return;
        }

        mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));
    }

    /**
     * Generate the details needed for the call notifications.
     *
     * @return
     */
    protected CallNotifications.CallNotificationDetail getCallNotificationDetails() {
        return new CallNotifications.CallNotificationDetail(getCallerInfo(), getCallerIdFromIntent(), getPhoneNumberFromIntent(), getIntent().getType());
    }

    private String getCallerInfo() {
        if (getCallerIdFromIntent() != null && !getCallerIdFromIntent().isEmpty()) {
            return getCallerIdFromIntent();
        }
        return getPhoneNumberFromIntent();
    }

    protected MediaManager getMediaManager() {
        return MediaManager.init(this, this, this);
    }

    protected boolean wasOpenedViaNotification() {
        if (getIntent() == null) {
            return false;
        }

        return getIntent().getBooleanExtra(NotificationHelper.TAG, false);
    }

    public SipServiceConnection getSipServiceConnection() {
        return mSipServiceConnection;
    }

    public String getCurrentCallId() {
        return mCurrentCallId;
    }
}

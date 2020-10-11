package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.view.WindowManager;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.audio.AudioRouter;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.LoginRequiredActivity;

import javax.inject.Inject;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.Optional;

public abstract class AbstractCallActivity extends LoginRequiredActivity implements
        SipServiceConnection.SipServiceConnectionListener, CallDurationTracker.Listener, CallStatusReceiver.Listener {

    protected SipServiceConnection mSipServiceConnection;
    protected CallDurationTracker mCallDurationTracker;
    protected CallStatusReceiver mCallStatusReceiver;
    protected PowerManager powerManager;
    protected PowerManager.WakeLock wakeLock;

    @Nullable @BindView(R.id.duration_text_view) TextView mCallDurationView;

    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VialerApplication.get().component().inject(this);
        mSipServiceConnection = new SipServiceConnection(this);
        mCallDurationTracker = new CallDurationTracker(mSipServiceConnection);
        mCallStatusReceiver = new CallStatusReceiver(this);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "vialer:in-call");
            wakeLock.acquire();
        }

        requestMicrophonePermissionIfNecessary();
        configureActivityFlags();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(mCallStatusReceiver, ACTION_BROADCAST_CALL_STATUS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSipServiceConnection.connect();
        mCallDurationTracker.start(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSipServiceConnection.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBroadcastReceiverManager.unregisterReceiver(mCallStatusReceiver);
        if (wakeLock != null) {
            wakeLock.release();
        }
    }

    @Override
    @CallSuper
    public void sipServiceHasConnected(SipService sipService) {
        if (sipService.getFirstCall() != null) {
        } else {
            finish();
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    protected void onPickupButtonClicked() {

    }

    protected void onDeclineButtonClicked() {

    }

    protected String getPhoneNumberFromIntent() {
        return getIntent().getStringExtra(PHONE_NUMBER);
    }

    protected String getCallerIdFromIntent() {
        return getIntent().getStringExtra(CONTACT_NAME);
    }

    @Optional
    public void onCallDurationUpdate(long seconds) {
        if (!mSipServiceConnection.isAvailableAndHasActiveCall() || mCallDurationView == null) {
            return;
        }

        mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));
    }

    private String getCallerInfo() {
        if (getCallerIdFromIntent() != null && !getCallerIdFromIntent().isEmpty()) {
            return getCallerIdFromIntent();
        }
        return getPhoneNumberFromIntent();
    }


    public SipServiceConnection getSipServiceConnection() {
        return mSipServiceConnection;
    }

    public AudioRouter getAudioRouter() {
        return getSipServiceConnection().get().getAudioRouter();
    }

    public static Intent createIntentForCallActivity(Context caller, Class<?> activity) {
        Intent intent = new Intent(caller, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}

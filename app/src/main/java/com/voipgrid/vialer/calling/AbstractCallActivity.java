package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.audio.AudioRouter;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.ProximitySensorHelper;

import javax.inject.Inject;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.Optional;

public abstract class AbstractCallActivity extends LoginRequiredActivity implements
        SipServiceConnection.SipServiceConnectionListener, CallDurationTracker.Listener, CallStatusReceiver.Listener {

    protected SipServiceConnection mSipServiceConnection;
    protected String mCurrentCallId;
    protected CallDurationTracker mCallDurationTracker;
    protected CallStatusReceiver mCallStatusReceiver;

    @Nullable @BindView(R.id.duration_text_view) TextView mCallDurationView;

    @Inject protected BroadcastReceiverManager mBroadcastReceiverManager;

    private ProximitySensorHelper mProximityHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VialerApplication.get().component().inject(this);
        mSipServiceConnection = new SipServiceConnection(this);
        mCallDurationTracker = new CallDurationTracker(mSipServiceConnection);
        mCallStatusReceiver = new CallStatusReceiver(this);
        mProximityHelper = new ProximitySensorHelper(this);

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
        mBroadcastReceiverManager.unregisterReceiver(mCallStatusReceiver);
    }

    @Override
    @CallSuper
    public void sipServiceHasConnected(SipService sipService) {
        if (sipService.getFirstCall() != null) {
            mCurrentCallId = sipService.getFirstCall().getIdentifier();
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
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
        if (!mSipServiceConnection.isAvailableAndHasActiveCall() || !mSipServiceConnection.get().getCurrentCall().isConnected() || mCallDurationView == null) {
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

    public String getCurrentCallId() {
        return mCurrentCallId;
    }

    public static Intent createIntentForCallActivity(Context caller, Class<?> activity, Uri sipAddressUri, String type, String callerId, String number) {
        Intent intent = new Intent(caller, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(sipAddressUri, type);
        intent.putExtra(CallingConstants.CONTACT_NAME, callerId);
        intent.putExtra(CallingConstants.PHONE_NUMBER, number);
        return intent;
    }
}

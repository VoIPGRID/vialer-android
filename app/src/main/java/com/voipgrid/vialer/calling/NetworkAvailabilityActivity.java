package com.voipgrid.vialer.calling;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.NetworkUtil;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class NetworkAvailabilityActivity extends AbstractCallActivity {

    @Inject CallActivityHelper mCallActivityHelper;
    @Inject NetworkUtil mNetworkUtil;
    Logger mLogger = new Logger (this);

    @BindView(R.id.incoming_caller_title) TextView mIncomingCallerTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mIncomingCallerSubtitle;
    @BindView(R.id.duration_text_view) TextView mCallDurationView;
    @BindView(R.id.profile_image) CircleImageView mContactImage;
    @BindView(R.id.button_hangup) ImageView mButtonHangup;

    private final static int CHECK_USER_IS_CONNECTED_TO_NETWORK = 500;

    private Handler mCheckServiceHandler = new Handler();
    private Runnable mCheckServiceRunnable = new Runnable() {
        @Override
        public void run() {
            // Check if the Network is restored every 5 seconds.
            checkNetworkRestored();
            mCheckServiceHandler.postDelayed(this, CHECK_USER_IS_CONNECTED_TO_NETWORK);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_availability);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mCheckServiceHandler.postDelayed(mCheckServiceRunnable, CHECK_USER_IS_CONNECTED_TO_NETWORK);
    }

    protected void checkNetworkRestored() {
        if(mNetworkUtil.isOnline()) {
            finish();
        }
    }

    @Override
    protected void onPickupButtonClicked() {

    }

    @OnClick(R.id.button_hangup)
    protected void onDeclineButtonClicked() {
        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            finish();
            return;
        }

        try {
            mSipServiceConnection.get().getCurrentCall().hangup(true);
            mLogger.i("The user hang up from Network Availability Activity");
        } catch (Exception e) {
            mLogger.i("Failed to decline call "+e.getClass().getSimpleName()+e.getMessage());
            finish();
        }
    }


    @Override
    public void sipServiceHasConnected(SipService sipService) {
        super.sipServiceHasConnected(sipService);

        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            finish();
            return;
        }

        SipCall sipCall = mSipServiceConnection.get().getCurrentCall();
        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mIncomingCallerTitle, mIncomingCallerSubtitle,sipCall.getPhoneNumber(), sipCall.getCallerId(), mContactImage);
    }

    public static void start() {
        Intent intent = new Intent(VialerApplication.get(), NetworkAvailabilityActivity.class);
        Logger logger = new Logger(NetworkAvailabilityActivity.class);
        VialerApplication.get().startActivity(intent);
        logger.d("No connectivity available, the Network Availability Activity is being shown");
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {

    }

    @Override
    public void onCallConnected() {

    }

    @Override
    public void onCallDisconnected() {

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
}

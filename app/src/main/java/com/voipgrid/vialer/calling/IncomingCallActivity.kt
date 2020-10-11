package com.voipgrid.vialer.calling;

import android.app.KeyguardManager;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.sip.CallDisconnectedReason;
import com.voipgrid.vialer.sip.SipService;

import javax.inject.Inject;

import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IncomingCallActivity extends AbstractCallActivity {

    @Inject KeyguardManager mKeyguardManager;
    @Inject Contacts mContacts;
    @Inject CallActivityHelper mCallActivityHelper;

    @BindView(R.id.incoming_caller_title) TextView mIncomingCallerTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mIncomingCallerSubtitle;
    @BindView(R.id.button_decline) ImageButton mButtonDecline;
    @BindView(R.id.button_pickup) ImageButton mButtonPickup;
    @BindView(R.id.call_buttons) View mCallButtons;
    @BindView(R.id.animation) ImageView animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mIncomingCallerTitle, mIncomingCallerSubtitle, getPhoneNumberFromIntent(), getCallerIdFromIntent());
        beginAnimation();
    }

    /**
     * Begin the incoming call animation and loop it.
     *
     */
    private void beginAnimation() {
        Drawable d = animation.getDrawable();
        ((AnimatedVectorDrawable) d).start();
        ((AnimatedVectorDrawable) d).registerAnimationCallback(
                new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(final Drawable drawable) {
                        super.onAnimationEnd(drawable);
                        new Handler().postDelayed(((AnimatedVectorDrawable) d)::start, 1000);
                    }
                });
    }

    @OnClick(R.id.button_decline)
    public void onDeclineButtonClicked() {
        getLogger().d("decline");

        disableAllButtons();

        if (!mSipServiceConnection.isAvailable()) {
            return;
        }

        if (mSipServiceConnection.get().getCurrentCall() == null) {
            return;
        }

        SipService.performActionOnSipService(this, SipService.Actions.DECLINE_INCOMING_CALL);

        finish();
    }

    @OnClick(R.id.button_pickup)
    public void onPickupButtonClicked() {
        if (!mSipServiceConnection.isAvailable() || mSipServiceConnection.get().getCurrentCall() == null) {
            finish();
            return;
        }

        disableAllButtons();

        SipService.performActionOnSipService(this, SipService.Actions.ANSWER_INCOMING_CALL);
    }

    private void disableAllButtons() {
        mButtonPickup.setEnabled(false);
        mButtonDecline.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSipServiceConnection.disconnect(true);
    }

    @Override
    public void sipServiceHasConnected(SipService sipService) {
        super.sipServiceHasConnected(sipService);
        if (sipService.getFirstCall() == null) finish();
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {

    }

    @Override
    public void onCallConnected() {
        mSipServiceConnection.disconnect(true);
    }

    @Override
    public void onCallDisconnected(CallDisconnectedReason reason) {
        finish();
    }
}
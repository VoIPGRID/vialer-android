package com.voipgrid.vialer.calling;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.LoginRequiredActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PendingCallActivity extends LoginRequiredActivity {

    public static final String EXTRA_DIALLED_NUMBER = "EXTRA_DIALLED_NUMBER";

    @Inject CallActivityHelper mCallActivityHelper;

    @BindView(R.id.lock_ring_container) View mLockRing;
    @BindView(R.id.call_state) TextView mCallState;
    @BindView(R.id.establishing_text_container) View mEstablishingTextContainer;
    @BindView(R.id.establishing_text) TextView mEstablishingText;
    @BindView(R.id.incoming_caller_title) TextView mCallerTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mCallerSubtitle;
    @BindView(R.id.call_buttons) View mCallButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);
        mLockRing.setVisibility(View.GONE);
        mCallButtons.setVisibility(View.GONE);
        mEstablishingTextContainer.setVisibility(View.VISIBLE);
        mCallState.setText(R.string.pending_call_description);
        mEstablishingText.setText(R.string.pending_call_status);
        String number = getIntent().getStringExtra(EXTRA_DIALLED_NUMBER);
        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mCallerTitle, mCallerSubtitle, number, null);
    }
}

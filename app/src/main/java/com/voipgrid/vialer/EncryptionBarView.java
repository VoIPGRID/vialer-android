package com.voipgrid.vialer;

import static com.voipgrid.vialer.api.SecureCalling.ACTION_SECURE_CALLING_API_CALL_RESPONSE;
import static com.voipgrid.vialer.api.SecureCalling.EXTRA_API_CALL_SUCCEEDED;
import static com.voipgrid.vialer.api.SecureCalling.EXTRA_API_CALL_WAS_ATTEMPTING_TO_ENABLE;
import static com.voipgrid.vialer.sip.SipConstants.ACTION_BROADCAST_CALL_STATUS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.voipgrid.vialer.api.SecureCalling;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EncryptionBarView extends RelativeLayout {

    @BindView(R.id.reachability_bar_text_view) TextView barText;
    @BindView(R.id.reachability_bar_drawable_info_icon) ImageView barImage;

    private static Receiver sReceiver;

    /**
     * This is a flag that determines when the bar should be rendered, while
     * this is set to FALSE, the view should be hidden regardless of all other
     * state.
     */
    protected static boolean sIsEligibleToDisplay = false;

    public EncryptionBarView(Context context) {
        super(context);
        initLayout(context);
    }

    public EncryptionBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public EncryptionBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    private void initLayout(Context context) {
        RelativeLayout bar = (RelativeLayout) LayoutInflater.from(context).inflate(
                R.layout.view_reachability_bar, this);

        SecureCalling secureCalling = SecureCalling.fromContext(context);

        ButterKnife.bind(bar);

        barText.setText(R.string.call_not_encrypted_warning_bar_text);
        barImage.setVisibility(GONE);
        bar.setVisibility(GONE);

        registerReceiver(context);

        if (!sIsEligibleToDisplay || secureCalling.isEnabled()) return;

        bar.setVisibility(VISIBLE);
    }

    /**
     * This bar should only appear between the time of a failed API request/user disabling encryption and the
     * next call. We are registering a permanent receiver that will listen for calls and for API responses
     * and set a flag so we know when we are within that window.
     *
     * @param context
     */
    private void registerReceiver(Context context) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

        if (sReceiver == null) {
            sReceiver = new Receiver();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BROADCAST_CALL_STATUS);
        intentFilter.addAction(ACTION_SECURE_CALLING_API_CALL_RESPONSE);

        localBroadcastManager.unregisterReceiver(sReceiver);
        localBroadcastManager.registerReceiver(sReceiver, intentFilter);
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_SECURE_CALLING_API_CALL_RESPONSE.equals(action)) {
                if (apiCallFailed(intent) || apiCallDisabledEncryption(intent)) {
                    sIsEligibleToDisplay = true;
                }
            }
            else if (ACTION_BROADCAST_CALL_STATUS.equals(action)) {
                sIsEligibleToDisplay = false;
            }
        }

        private boolean apiCallFailed(Intent intent) {
            return !intent.getBooleanExtra(EXTRA_API_CALL_SUCCEEDED, true);
        }

        private boolean apiCallDisabledEncryption(Intent intent) {
            return !intent.getBooleanExtra(EXTRA_API_CALL_WAS_ATTEMPTING_TO_ENABLE, false);
        }
    }
}

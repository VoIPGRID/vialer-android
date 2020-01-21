package com.voipgrid.vialer.reachability;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.util.ConnectivityHelper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

/**
 * Class that handles the network state view and allows the updating of the view
 * according to the current connection.
 */
public class ReachabilityBarView extends RelativeLayout implements View.OnClickListener, ReachabilityInterface {
    private Context mContext;
    private ConnectivityHelper mConnectivityHelper;
    private TextView mReachabilityBarTextView;
    private ImageView mReachabilityInfoImageView;
    private RelativeLayout mReachabilityBarView;
    private LinearLayout mReachabilityHolder;
    private TextView mReachabilitySettingsTextView;

    public ReachabilityBarView(Context context) {
        super(context);
        mContext = context;
        initLayout();
    }

    public ReachabilityBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initLayout();
    }

    public ReachabilityBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initLayout();
    }

    public ReachabilityBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        initLayout();
    }

    private void initLayout() {
        mReachabilityBarView = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.view_reachability_bar, this);

        mReachabilityHolder = mReachabilityBarView.findViewById(R.id.reachability_holder);
        mReachabilityBarTextView = mReachabilityBarView.findViewById(R.id.reachability_bar_text_view);
        mReachabilityInfoImageView = mReachabilityBarView.findViewById(R.id.reachability_bar_drawable_info_icon);
        mReachabilitySettingsTextView = mReachabilityBarView.findViewById(R.id.reachability_bar_settings);

        mReachabilityInfoImageView.setOnClickListener(this);

        if (!isInEditMode()) {
            mConnectivityHelper = ConnectivityHelper.get(mContext);
        } else {
            mReachabilityBarTextView.setText(R.string.dialer_warning_voip_disabled);
        }

        ReachabilityReceiver.setInterfaceCallback(this);
    }

    public void setEncryptionView() {
        mReachabilityHolder.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.encryption_rounded_background));
        mReachabilityBarTextView.setText(getResources().getString(R.string.not_encrypted));
        mReachabilityBarTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.dial_button_digit_color));
        mReachabilityInfoImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_lock_open));
        mReachabilityInfoImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.dial_button_chars_color), PorterDuff.Mode.SRC_IN);
        mReachabilitySettingsTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.dial_button_digit_color));
    }

    /**
     * Function to update the network state view for the current network state.
     */
    public void updateNetworkStateView() {
        boolean shouldBeVisible = true;
        boolean showInfoImageView = false;

        if(!mConnectivityHelper.hasNetworkConnection()) {
            // The user doesn't have any connection.
            mReachabilityBarTextView.setText(R.string.dialer_warning_no_connection);
            showInfoImageView = true;
        } else if (!mConnectivityHelper.hasFastData()) {
            // The connection is not fast enough.
            // SIP is enabled and allowed to use, show the click-to-dial message.
            if (User.voip.getHasEnabledSip() && User.voip.isAccountSetupForSip()) {
                mReachabilityBarTextView.setText(R.string.dialer_warning_a_b_connect);
                showInfoImageView = true;
            } else {
                // User has disabled the VoIP switch.
                mReachabilityBarTextView.setText(R.string.dialer_warning_voip_disabled);
            }
        } else if (mConnectivityHelper.hasFastData()) {
            // The user has fast enough connection for SIP calling
            if (!(User.voip.getHasEnabledSip() && User.voip.isAccountSetupForSip())) {
                // The user has disabled the VoIP switch.
                mReachabilityBarTextView.setText(R.string.dialer_warning_voip_disabled);
            } else {
                shouldBeVisible = false;
            }
        } else {
            shouldBeVisible = false;
        }

        if (showInfoImageView) {
            mReachabilityInfoImageView.setVisibility(View.VISIBLE);
        } else {
            mReachabilityInfoImageView.setVisibility(View.GONE);
        }
        mReachabilityBarView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);

    }

    @Override
    public void onClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.dialer_connect_a_b_info)
                .setTitle(R.string.dialer_connect_a_b_info_title)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void networkChange() {
        updateNetworkStateView();
    }
}

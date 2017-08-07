package com.voipgrid.vialer.reachability;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;

/**
 * Class that handles the network state view and allows the updating of the view
 * according to the current connection.
 */
public class ReachabilityBarView extends RelativeLayout implements View.OnClickListener, ReachabilityInterface {
    private Context mContext;
    private ConnectivityHelper mConnectivityHelper;
    private JsonStorage mJsonStorage;
    private Preferences mPreferences;
    private TextView mReachabilityBarTextView;
    private ImageView mReachabilityInfoImageView;
    private RelativeLayout mReachabilityBarView;

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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReachabilityBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        initLayout();
    }

    private void initLayout() {
        mReachabilityBarView = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.view_reachability_bar, this);

        mReachabilityBarTextView = (TextView) mReachabilityBarView.findViewById(R.id.reachability_bar_text_view);
        mReachabilityInfoImageView = (ImageView) mReachabilityBarView.findViewById(R.id.reachability_bar_drawable_info_icon);

        mReachabilityInfoImageView.setOnClickListener(this);

        if (!isInEditMode()) {
            mConnectivityHelper = ConnectivityHelper.get(mContext);
            mPreferences = new Preferences(mContext);
        } else {
            mReachabilityBarTextView.setText(R.string.dialer_warning_voip_disabled);
        }

        ReachabilityReceiver.setInterfaceCallback(this);
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
        } else if (!mConnectivityHelper.hasFastData()) {
            // The connection is not fast enough.
            // SIP is enabled and allowed to use, show the click-to-dial message.
            if (mPreferences.hasSipEnabled() && mPreferences.hasSipPermission()) {
                mReachabilityBarTextView.setText(R.string.dialer_warning_a_b_connect);
                showInfoImageView = true;
            } else {
                // User has disabled the VoIP switch.
                mReachabilityBarTextView.setText(R.string.dialer_warning_voip_disabled);
            }
        } else if (mConnectivityHelper.hasFastData()) {
            // The user has fast enough connection for SIP calling
            if (!(mPreferences.hasSipEnabled() && mPreferences.hasSipPermission())) {
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

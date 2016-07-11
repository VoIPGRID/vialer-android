package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.view.View;
import android.widget.TextView;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.WarningActivity;
import com.voipgrid.vialer.api.models.PhoneAccount;

/**
 * Class that handles the network state view and allows the updating of the view
 * according to the current connection.
 */
public class NetworkStateViewHelper extends BroadcastReceiver implements View.OnClickListener {
    private Context mContext;
    private ConnectivityHelper mConnectivityHelper;
    private JsonStorage mJsonStorage;
    private Preferences mPreferences;
    private TextView mNetworkStateView;

    /**
     * Constructor
     * @param context
     * @param networkStateView The view that belongs to R.id.dialer_warning
     */
    public NetworkStateViewHelper(Context context, TextView networkStateView) {
        mContext = context;
        mNetworkStateView = networkStateView;
        mConnectivityHelper = ConnectivityHelper.get(mContext);
        mJsonStorage = new JsonStorage(mContext);
        mPreferences = new Preferences(mContext);
        mNetworkStateView.setOnClickListener(this);
    }

    /**
     * Function to start listening for connectivity changes.
     */
    public void startListening() {
        mContext.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Function to stop listening for connectivity changes.
     */
    public void stopListening() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateNetworkStateView();
    }

    /**
     * Function to update the network state view for the current network state.
     */
    public void updateNetworkStateView() {
        mNetworkStateView.setVisibility(View.VISIBLE);
        if(!mConnectivityHelper.hasNetworkConnection()) {
            mNetworkStateView.setText(R.string.dialer_warning_no_connection);
            mNetworkStateView.setTag(mContext.getString(R.string.dialer_warning_no_connection_message));
        } else if(!mConnectivityHelper.hasFastData() && mPreferences.canUseSip()) {
            mNetworkStateView.setText(R.string.dialer_warning_a_b_connect);
            mNetworkStateView.setTag(mContext.getString(R.string.dialer_warning_a_b_connect_connectivity_message));
        } else if(!mJsonStorage.has(PhoneAccount.class) && mPreferences.canUseSip()) {
            mNetworkStateView.setText(R.string.dialer_warning_a_b_connect);
            mNetworkStateView.setTag(mContext.getString(R.string.dialer_warning_a_b_connect_account_message));
        } else {
            mNetworkStateView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(mContext, WarningActivity.class);
        intent.putExtra(WarningActivity.TITLE, ((TextView) view).getText());
        intent.putExtra(WarningActivity.MESSAGE, (String) view.getTag());
        mContext.startActivity(intent);
    }
}

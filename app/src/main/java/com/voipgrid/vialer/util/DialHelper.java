package com.voipgrid.vialer.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.twostepcall.TwoStepCallActivity;

/**
 * Helper class to use to setup a outgoing call. Based on connectivity choose between SIP or
 * A/B connect
 */
public class DialHelper {

    private Context mContext;

    private AnalyticsHelper mAnalyticsHelper;
    private ConnectivityHelper mConnectivityHelper;
    private final Preferences mPreferences;
    private JsonStorage mJsonStorage;

    private int mMaximumNetworkSwitchDelay = 3000;

    public DialHelper(Context context, JsonStorage jsonStorage,
            ConnectivityHelper connectivityHelper, AnalyticsHelper analyticsHelper ) {
        mContext = context;
        mJsonStorage = jsonStorage;
        mConnectivityHelper = connectivityHelper;
        mAnalyticsHelper = analyticsHelper;
        mPreferences = new Preferences(context);
    }

    public void callNumber(final String number, final String contactName) {
        if(mConnectivityHelper.getConnectionType() == ConnectivityHelper.Connection.WIFI && mPreferences.hasSipEnabled()) {
            if(mPreferences.hasConnectionPreference(ConnectivityHelper.Connection.LTE.toInt())) {
                switchNetworkAndCallNumber(number, contactName);
            } else if(mPreferences.hasConnectionPreference(Preferences.CONNECTION_PREFERENCE_NONE)) {
                showConnectionPickerDialog(number, contactName);
            } else if(mPreferences.hasConnectionPreference(Preferences.CONNECTION_PREFERENCE_WIFI)) {
                makeCall(number, contactName);
            }
        } else {
            makeCall(number, contactName);
        }
    }

    private void switchNetworkAndCallNumber(final String number, final String contactName) {
        mConnectivityHelper.attemptUsingLTE(mContext, mMaximumNetworkSwitchDelay);
        Toast.makeText(mContext, mContext.getString(R.string.connection_preference_switch_toast), Toast.LENGTH_LONG).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                makeCall(number, contactName);
            }
        }, mMaximumNetworkSwitchDelay);
    }

    private void showConnectionPickerDialog(final String number, final String contactName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.connection_preference_dialog_title));
        builder.setMessage(mContext.getString(R.string.connection_preference_dialog_message));
        builder.setPositiveButton(mContext.getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        switchNetworkAndCallNumber(number, contactName);
                    }
                });
        builder.setNegativeButton(mContext.getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        makeCall(number, contactName);
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Setup a connection for the given number. When an PhoneAccount and a fast data connection
     * are both available the call will be setup using SIP otherwise make an API call to setup the
     * connection through A/B connect
     * @param number
     * @param contactName
     */
    public void makeCall(String number, String contactName) {
        // We need internet for both type of calls.
        if (mConnectivityHelper.hasNetworkConnection()) {
            if (mPreferences.canUseSip()
                    && mJsonStorage.has(PhoneAccount.class)
                    && mConnectivityHelper.hasFastData()) {
                // Check if we have permission to use the microphone. If not, request it.
                if (!MicrophonePermission.hasPermission(mContext)) {
                    MicrophonePermission.askForPermission((Activity) mContext);
                    return;
                }
                callWithSip(number, contactName);
            } else {
                callWithApi(number, contactName);
            }
        }
    }

    /**
     * Setup a call using SIP
     * @param number
     * @param contactName
     */
    private void callWithSip(String number, String contactName) {
        Intent intent = new Intent(mContext, SipService.class);
        intent.setAction(SipConstants.ACTION_CALL_OUTGOING);

        // set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                mContext,
                PhoneNumberUtils.format(number)
        );
        intent.setData(sipAddressUri);

        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, number);
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, contactName);

        mContext.startService(intent);

        mAnalyticsHelper.sendEvent(
                mContext.getString(R.string.analytics_event_category_call),
                mContext.getString(R.string.analytics_event_action_outbound),
                mContext.getString(R.string.analytics_event_label_sip)
        );
    }

    /**
     * Setup a call using A/B connect
     * @param number
     * @param contact
     */
    private void callWithApi(String number, String contact) {
        Intent intent = new Intent(mContext, TwoStepCallActivity.class);
        intent.putExtra(TwoStepCallActivity.NUMBER_TO_CALL, number);
        mContext.startActivity(intent);
    }
}

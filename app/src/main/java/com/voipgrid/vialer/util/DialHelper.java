package com.voipgrid.vialer.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.persistence.UserPreferences;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.sip.core.Action;
import com.voipgrid.vialer.twostepcall.TwoStepCallActivity;

import androidx.appcompat.app.AlertDialog;

/**
 * Helper class to use to setup a outgoing call. Based on connectivity choose between SIP or
 * A/B connect
 */
public class DialHelper {

    private Context mContext;

    private ConnectivityHelper mConnectivityHelper;

    private int mMaximumNetworkSwitchDelay = 3000;
    private static String sNumberAttemptedToCall;

    private DialHelper(Context context, ConnectivityHelper connectivityHelper) {
        mContext = context;
        mConnectivityHelper = connectivityHelper;
    }

    public static DialHelper fromActivity(Activity activity) {
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(activity);
        return new DialHelper(activity, connectivityHelper);
    }

    public void callNumber(final String number, final String contactName) {
        if (mConnectivityHelper.getConnectionType() == ConnectivityHelper.Connection.WIFI
                && User.voip.getHasEnabledSip()) {
            if (User.userPreferences.hasConnectionPreference(
                    UserPreferences.ConnectionPreference.ONLY_CELLULAR)) {
                switchNetworkAndCallNumber(number, contactName);
            } else if (User.userPreferences.hasConnectionPreference(
                    UserPreferences.ConnectionPreference.CEULLAR_AND_WIFI)) {
                makeCall(number, contactName);
            } else if (User.userPreferences.hasConnectionPreference(
                    UserPreferences.ConnectionPreference.SHOW_POPUP_BEFORE_EVERY_CALL)) {
                showConnectionPickerDialog(number, contactName);
            }
        } else {
            makeCall(number, contactName);
        }
    }

    private void switchNetworkAndCallNumber(final String number, final String contactName) {
        mConnectivityHelper.attemptUsingLTE(mContext, mMaximumNetworkSwitchDelay);
        Toast.makeText(mContext, mContext.getString(R.string.connection_preference_switch_toast),
                Toast.LENGTH_LONG).show();
        new Handler().postDelayed(() -> makeCall(number, contactName), mMaximumNetworkSwitchDelay);
    }

    private void showConnectionPickerDialog(final String number, final String contactName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.connection_preference_dialog_title));
        builder.setMessage(mContext.getString(R.string.connection_preference_dialog_message));
        builder.setPositiveButton(mContext.getString(R.string.yes),
                (dialog, id) -> {
                    dialog.cancel();
                    switchNetworkAndCallNumber(number, contactName);
                });
        builder.setNegativeButton(mContext.getString(R.string.no),
                (dialog, id) -> {
                    dialog.cancel();
                    makeCall(number, contactName);
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
            if (User.voip.getCanUseSip()
                    && User.getHasVoipAccount()
                    && mConnectivityHelper.hasFastData()) {
                // Check if we have permission to use the microphone. If not, request it.
                if (!MicrophonePermission.hasPermission((Activity) mContext)) {
                    sNumberAttemptedToCall = number;
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
     * Dial number requested before the first microphone permission.
     */
    public void callAttemptedNumber() {
        if (sNumberAttemptedToCall == null) return;
        makeCall(sNumberAttemptedToCall, "");
        sNumberAttemptedToCall = null;
    }

    /**
     * Setup a call using SIP
     * @param number
     * @param contactName
     */
    private void callWithSip(String number, String contactName) {
        Intent intent = new Intent(mContext, SipService.class);
        intent.setAction(Action.HANDLE_OUTGOING_CALL.toString());

        // set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                mContext,
                PhoneNumberUtils.format(number)
        );
        intent.setData(sipAddressUri);

        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, number);
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, contactName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(intent);
        } else {
            mContext.startService(intent);
        }
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

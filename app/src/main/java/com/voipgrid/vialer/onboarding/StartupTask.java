package com.voipgrid.vialer.onboarding;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.squareup.okhttp.OkHttpClient;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerGcmRegistrationService;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.Middleware;
import com.voipgrid.vialer.util.JsonStorage;

import java.io.IOException;

import retrofit.RetrofitError;
import retrofit.client.OkClient;

/**
 * Created by eltjo on 04/08/15.
 */
public class StartupTask extends AsyncTask {

    private final ConnectivityHelper mConnectivityHelper;
    private Preferences mPreferences;

    private Context mContext;

    private Api mApi;

    private JsonStorage mJsonStorage;

    public StartupTask(Context context) {
        mContext = context;

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE)
        );

        mPreferences = new Preferences(context);

        mJsonStorage = new JsonStorage(context);

        /* get username and password */
        SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        String username = systemUser.getEmail();
        String password = systemUser.getPassword();

        mApi = ServiceGenerator.createService(
                mContext,
                mConnectivityHelper,
                Api.class,
                context.getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(mContext, username, password))
        );
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            SystemUser systemUser = mApi.systemUser();
            mPreferences.setSipPermission(systemUser.hasSipPermission());
            String phoneAccountId = systemUser.getPhoneAccountId();
            PhoneAccount phoneAccount = ((PhoneAccount) new JsonStorage(mContext).get(PhoneAccount.class));
            if (phoneAccountId != null) {
                phoneAccount = mApi.phoneAccount(phoneAccountId);
                mJsonStorage.save(phoneAccount);
                if(mPreferences.hasSipPermission()) {
                    mContext.startService(new Intent(mContext, VialerGcmRegistrationService.class));
                }
            } else if (phoneAccount != null && mPreferences.hasSipPermission()) {
                Middleware.unregister(mContext);
            }
        } catch (RetrofitError e) {
            // Setup can fail. No need to handle error. Next startup the app will try again.
        } catch (IOException i) {
            // Registration failed.
        }
        return null;
    }
}

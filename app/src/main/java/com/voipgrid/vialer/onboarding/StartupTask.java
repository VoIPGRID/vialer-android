package com.voipgrid.vialer.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerGcmRegistrationService;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.Middleware;
import com.voipgrid.vialer.util.JsonStorage;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;


/**
 * Created by eltjo on 04/08/15.
 */
public class StartupTask extends AsyncTask {

    private Preferences mPreferences;

    private Context mContext;

    private Api mApi;

    private JsonStorage mJsonStorage;

    public StartupTask(Context context) {
        mContext = context;

        mPreferences = new Preferences(context);

        mJsonStorage = new JsonStorage(context);

        /* get username and password */
        SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        String username = systemUser.getEmail();
        String password = systemUser.getPassword();

        mApi = ServiceGenerator.createService(
                mContext,
                Api.class,
                context.getString(R.string.api_url),
                username,
                password
        );
    }

    @Override
    protected Object doInBackground(Object[] params) {
        Call<SystemUser> call = mApi.systemUser();
        try {
            // Get the systemuser.
            Response<SystemUser> response = call.execute();
            if (response.isSuccess() && response.body() != null) {
                SystemUser systemUser = response.body();
                // Set the permissions based on systemuser.
                mPreferences.setSipPermission(systemUser.hasSipPermission());
                String phoneAccountId = systemUser.getPhoneAccountId();
                PhoneAccount phoneAccount = ((PhoneAccount) new JsonStorage(mContext).get(PhoneAccount.class));
                // Get phoneaccount from API if one is provided in the systemuser API.
                if (phoneAccountId != null) {
                    Call<PhoneAccount> phoneAccountCall = mApi.phoneAccount(phoneAccountId);
                    Response<PhoneAccount> phoneAccountResponse = phoneAccountCall.execute();
                    if (phoneAccountResponse.isSuccess() && phoneAccountResponse.body() != null) {
                        phoneAccount = phoneAccountResponse.body();
                        // Save the (app)phoneaccount.
                        mJsonStorage.save(phoneAccount);
                        if(mPreferences.hasSipPermission()) {
                            // Start service for incoming calls on push wakeup.
                            mContext.startService(new Intent(mContext, VialerGcmRegistrationService.class));
                        }
                    }
                } else if (phoneAccount != null && mPreferences.hasSipPermission()) {
                    // No phone account so unregister at middleware.
//                    Middleware.unregister(mContext);
                }
            }
        } catch (IOException e) {

        }
        return null;
    }
}

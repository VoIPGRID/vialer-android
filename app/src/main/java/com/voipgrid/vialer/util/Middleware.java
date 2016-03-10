package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Created by bwiegmans on 06/10/15.
 *
 * Handle (un)registration from the middleware in a centralised place.
 */
public class Middleware {

    public interface Constants {
        String REGISTRATION_STATUS = "VIALER_REGISTRATION_STATUS";
        String LAST_REGISTRATION = "VIALER_LAST_REGISTRATION";
        String CURRENT_TOKEN = "VIALER_CURRENT_TOKEN";
        /* Possible registration status values */
        int STATUS_UNREGISTERED = 0;
        int STATUS_REGISTERED = 1;
        int STATUS_FAILED = -1;
    }

    public static void register(Context context, String token) {
        Preferences sipPreferences = new Preferences(context);

        if (!sipPreferences.canUseSip()) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putLong(Constants.LAST_REGISTRATION, System.currentTimeMillis());

        JsonStorage jsonStorage = new JsonStorage(context);
        SystemUser systemUser = (SystemUser) jsonStorage.get(SystemUser.class);

        Registration api = ServiceGenerator.createService(
                context,
                Registration.class,
                getBaseApiUrl(context),
                systemUser.getEmail(),
                systemUser.getPassword()
        );
        String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
        String fullName = ((SystemUser) jsonStorage.get(SystemUser.class)).getFullName();
        String appName = context.getPackageName();
        Call<ResponseBody> call = api.register(fullName, token, sipUserId, Build.VERSION.CODENAME,
                Build.VERSION.RELEASE, appName);
        editor.putString(Constants.CURRENT_TOKEN, token);
        try {
            if (call.execute().isSuccess()) {
                editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_REGISTERED);
            } else {
                editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_FAILED);
            }
        } catch (IOException e) {
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_FAILED);
            e.printStackTrace();
        } finally {
            editor.apply();
        }
    }

    public static void unregister(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        JsonStorage jsonStorage = new JsonStorage(context);
        SystemUser systemUser = (SystemUser) jsonStorage.get(SystemUser.class);

        Registration api = ServiceGenerator.createService(
                context,
                Registration.class,
                getBaseApiUrl(context),
                systemUser.getEmail(),
                systemUser.getPassword());
        String token = preferences.getString(Constants.CURRENT_TOKEN, "");
        String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
        String appName = context.getPackageName();
        Call<ResponseBody> call = api.unregister(token, sipUserId, appName);
        try {
            if (call.execute().isSuccess()) {
                editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_UNREGISTERED);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            editor.apply();
        }
    }

    /**
     * Function to get the URL needed for api calls.
     * @param mContext
     * @return
     */
    public static String getBaseApiUrl(Context mContext) {
        return mContext.getString(R.string.registration_url);
    }

}

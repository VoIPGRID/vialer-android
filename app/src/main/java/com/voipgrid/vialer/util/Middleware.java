package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;

import java.io.IOException;

import retrofit.RetrofitError;
import retrofit.client.OkClient;

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

    public static void register(Context context, String token) throws IOException {
        Preferences sipPreferences = new Preferences(context);

        if (!sipPreferences.canUseSip()) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putLong(Constants.LAST_REGISTRATION, System.currentTimeMillis());

        try {
            JsonStorage jsonStorage = new JsonStorage(context);
            SystemUser systemUser = (SystemUser) jsonStorage.get(SystemUser.class);

            Registration api = ServiceGenerator.createService(
                    context,
                    ConnectivityHelper.get(context),
                    Registration.class,
                    getBaseApiUrl(context),
                    new OkClient(ServiceGenerator.getOkHttpClient(
                            context,
                            systemUser.getEmail(),
                            systemUser.getPassword()
                    ))
            );
            String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
            String fullName = ((SystemUser) jsonStorage.get(SystemUser.class)).getFullName();
            String appName = context.getPackageName();
            api.register(fullName, token, sipUserId, Build.VERSION.CODENAME,
                    Build.VERSION.RELEASE, appName);
            editor.putString(Constants.CURRENT_TOKEN, token);
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_REGISTERED);
        } catch (RetrofitError|NullPointerException error) {
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_FAILED);
            throw new IOException(error);
        } finally {
            editor.apply();
        }
    }

    public static void unregister(Context context) throws IOException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        try {
            JsonStorage jsonStorage = new JsonStorage(context);
            SystemUser systemUser = (SystemUser) jsonStorage.get(SystemUser.class);

            Registration api = ServiceGenerator.createService(
                    context,
                    ConnectivityHelper.get(context),
                    Registration.class,
                    getBaseApiUrl(context),
                    new OkClient(ServiceGenerator.getOkHttpClient(
                            context,
                            systemUser.getEmail(),
                            systemUser.getPassword()
                    ))
            );
            String token = preferences.getString(Constants.CURRENT_TOKEN, "");
            String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
            String appName = context.getPackageName();
            api.unregister(token, sipUserId, appName);
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_UNREGISTERED);
        } catch (RetrofitError|NullPointerException error) {
            throw new IOException(error);
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

package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.squareup.okhttp.OkHttpClient;
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(Constants.LAST_REGISTRATION, System.currentTimeMillis());

        try {
            Registration api = ServiceGenerator.createService(
                    context,
                    ConnectivityHelper.get(context),
                    Registration.class,
                    context.getString(R.string.registration_url),
                    new OkClient(new OkHttpClient())
            );
            Storage storage = new Storage(context);
            String sipUserId = ((PhoneAccount) storage.get(PhoneAccount.class)).getAccountId();
            String fullName = ((SystemUser) storage.get(SystemUser.class)).getFullName();
            String appName = context.getPackageName();
            api.register(fullName, token, sipUserId, Build.VERSION.CODENAME,
                    Build.VERSION.RELEASE, appName);
            editor.putString(Constants.CURRENT_TOKEN, token);
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_REGISTERED);
        } catch (RetrofitError|NullPointerException error) {
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_FAILED);
            throw new IOException(error);
        } finally {
            editor.commit();
        }
    }

    public static void unregister(Context context) throws IOException {
        SharedPreferences preferences   = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        try {
            Registration api = ServiceGenerator.createService(
                    context,
                    ConnectivityHelper.get(context),
                    Registration.class,
                    context.getString(R.string.registration_url),
                    new OkClient(new OkHttpClient())
            );
            String token     = preferences.getString(Constants.CURRENT_TOKEN, "");
            String sipUserId = (new Storage<PhoneAccount>(context).get(PhoneAccount.class))
                    .getAccountId();
            api.unregister(token, sipUserId);
            editor.putInt(Constants.REGISTRATION_STATUS, Constants.STATUS_UNREGISTERED);
        } catch (RetrofitError|NullPointerException error) {
            throw new IOException(error);
        } finally {
            editor.commit();
        }

    }
}

package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
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
public class MiddlewareHelper {

    public interface Constants {
        String REGISTRATION_STATUS = "VIALER_REGISTRATION_STATUS";
        String LAST_REGISTRATION = "VIALER_LAST_REGISTRATION";
        String CURRENT_TOKEN = "VIALER_CURRENT_TOKEN";
        /* Possible registration status values */
        int STATUS_UNREGISTERED = 0;
        int STATUS_REGISTERED = 1;
        int STATUS_FAILED = -1;
        int STATUS_UPDATE_NEEDED = 2;
    }

    /**
     * Function to set the current registration status with the middleware.
     * @param context
     * @param status
     */
    public static void setRegistrationStatus(Context context, int status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(Constants.REGISTRATION_STATUS, status).apply();
    }

    /**
     * Function to check if the app is currently registered at the middleware.
     * @param context
     * @return
     */
    public static boolean isRegistered(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentRegistration = prefs.getInt(Constants.REGISTRATION_STATUS,
                Constants.STATUS_UNREGISTERED);
        return currentRegistration == Constants.STATUS_REGISTERED;
    }

    public static boolean needsUpdate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentRegistration = prefs.getInt(Constants.REGISTRATION_STATUS,
                Constants.STATUS_UNREGISTERED);
        return currentRegistration == Constants.STATUS_UPDATE_NEEDED;
    }

    public static boolean needsRegistration(Context context) {
        return !isRegistered(context) || needsUpdate(context);
    }

    public static void register(Context context, String token) {
        Preferences sipPreferences = new Preferences(context);
        ((AnalyticsApplication) context.getApplicationContext()).getDefaultTracker();
        AnalyticsHelper analyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) context.getApplicationContext()).getDefaultTracker());

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
                setRegistrationStatus(context, Constants.STATUS_REGISTERED);
            } else {
                setRegistrationStatus(context, Constants.STATUS_FAILED);
                // Disable sip because failed at middleware.
                new Preferences(context).setSipEnabled(false);

                analyticsHelper.sendException(
                        context.getString(
                                R.string.analytics_event_description_registration_failed
                        )
                );
            }
        } catch (IOException e) {
            setRegistrationStatus(context, Constants.STATUS_FAILED);
            // Disable sip because failed at middleware.
            new Preferences(context).setSipEnabled(false);
            e.printStackTrace();
        } finally {
            editor.apply();
        }
    }

    /**
     * Function to synchronously unregister at the middleware if a phone account and
     * token are present.
     * @param context
     */
    public static void unregister(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        String token = preferences.getString(Constants.CURRENT_TOKEN, "");

        // Check if we have a phone account and a push token.
        if (new Preferences(context).hasPhoneAccount() && !token.equals("")) {
            JsonStorage jsonStorage = new JsonStorage(context);
            SystemUser systemUser = (SystemUser) jsonStorage.get(SystemUser.class);

            Registration api = ServiceGenerator.createService(
                    context,
                    Registration.class,
                    getBaseApiUrl(context),
                    systemUser.getEmail(),
                    systemUser.getPassword());
            String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
            String appName = context.getPackageName();
            Call<ResponseBody> call = api.unregister(token, sipUserId, appName);
            try {
                if (call.execute().isSuccess()) {
                    setRegistrationStatus(context, Constants.STATUS_UNREGISTERED);
                } else {
                    setRegistrationStatus(context, Constants.STATUS_FAILED);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                editor.apply();
            }
        } else {
            setRegistrationStatus(context, Constants.STATUS_FAILED);
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

    public static void executeUnregisterTask(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                unregister(context);
                return null;
            }
        }.execute();
    }
}

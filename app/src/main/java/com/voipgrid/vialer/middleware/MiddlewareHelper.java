package com.voipgrid.vialer.middleware;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.google.android.gms.analytics.Tracker;
import com.google.firebase.iid.FirebaseInstanceId;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.util.AccountHelper;
import com.voipgrid.vialer.util.JsonStorage;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.voipgrid.vialer.middleware.MiddlewareConstants.CURRENT_TOKEN;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.LAST_REGISTRATION;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.REGISTRATION_STATUS;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.STATUS_FAILED;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.STATUS_REGISTERED;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.STATUS_UNREGISTERED;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.STATUS_UPDATE_NEEDED;

/**
 * Handle (un)registration from the middleware in a centralised place.
 */
public class MiddlewareHelper {
    /**
     * Function to set the current registration status with the middleware.
     *
     * @param context Context
     * @param status Save the registration status with the middleware.
     */
    public static void setRegistrationStatus(Context context, int status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(REGISTRATION_STATUS, status).apply();
    }

    /**
     * Function to check if the app is currently registered at the middleware.
     *
     * @param context Context
     *
     * @return boolean if the app is still registered with the middleware.
     */
    public static boolean isRegistered(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentRegistration = prefs.getInt(
                REGISTRATION_STATUS, STATUS_UNREGISTERED
        );
        return currentRegistration == STATUS_REGISTERED;
    }

    private static boolean needsUpdate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentRegistration = prefs.getInt(
                REGISTRATION_STATUS, STATUS_UNREGISTERED
        );
        return currentRegistration == STATUS_UPDATE_NEEDED;
    }

    public static boolean needsRegistration(Context context) {
        return !isRegistered(context) || needsUpdate(context);
    }

    public static void register(final Context context, String token) {
        Preferences sipPreferences = new Preferences(context);
        Tracker analyticsTracker = ((AnalyticsApplication) context.getApplicationContext()).getDefaultTracker();
        final AnalyticsHelper analyticsHelper = new AnalyticsHelper(analyticsTracker);

        if (!sipPreferences.canUseSip()) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong(LAST_REGISTRATION, System.currentTimeMillis());

        JsonStorage jsonStorage = new JsonStorage(context);
        AccountHelper accountHelper = new AccountHelper(context);
        Preferences preferences = new Preferences(context);

        if (!jsonStorage.has(PhoneAccount.class) || !jsonStorage.has(SystemUser.class)) {
            return;
        }

        Registration api = ServiceGenerator.createRegistrationService(context);

        String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
        String fullName = ((SystemUser) jsonStorage.get(SystemUser.class)).getFullName();
        String appName = context.getPackageName();
        Call<ResponseBody> call = api.register(
                fullName,
                token,
                sipUserId,
                Build.VERSION.CODENAME,
                Build.VERSION.RELEASE,
                appName,
                (preferences.remoteLoggingIsActive() ? preferences.getLoggerIdentifier() : null)
        );
        editor.putString(CURRENT_TOKEN, token);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    setRegistrationStatus(context, STATUS_REGISTERED);
                } else {
                    setRegistrationStatus(context, STATUS_FAILED);
                    analyticsHelper.sendException(
                            context.getString(
                                    R.string.analytics_event_description_registration_failed
                            )
                    );
                }
                editor.apply();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                t.printStackTrace();
                setRegistrationStatus(context, STATUS_FAILED);
            }
        });
    }

    /**
     * Function to synchronously unregister at the middleware if a phone account and
     * token are present.
     * @param context
     */
    public static void unregister(final Context context) {
        final Logger logger = new Logger(MiddlewareHelper.class);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String token = preferences.getString(CURRENT_TOKEN, "");

        // Check if we have a phone account and a push token.
        if (new Preferences(context).hasPhoneAccount() && !token.equals("")) {
            JsonStorage jsonStorage = new JsonStorage(context);
            AccountHelper accountHelper = new AccountHelper(context);

            Registration api = ServiceGenerator.createRegistrationService(context);
            String sipUserId = ((PhoneAccount) jsonStorage.get(PhoneAccount.class)).getAccountId();
            String appName = context.getPackageName();

            Call<ResponseBody> call = api.unregister(token, sipUserId, appName);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        logger.d("unregister successful");
                        setRegistrationStatus(context, STATUS_UNREGISTERED);
                    } else {
                        logger.d("unregister failed");
                        setRegistrationStatus(context, STATUS_FAILED);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    logger.d("unregister failed");
                    setRegistrationStatus(context, STATUS_FAILED);
                }
            });
        } else {
            logger.d("No token or phone account so unregister");
            setRegistrationStatus(context, STATUS_FAILED);
        }
    }

    /**
     * Function to get the URL needed for api calls.
     *
     * @param context Context
     *
     * @return String the base API url.
     */
    public static String getBaseApiUrl(Context context) {
        return context.getString(R.string.registration_url);
    }

    /**
     * Register token at the middleware.
     *
     * @param context Context
     */
    public static void registerAtMiddleware(Context context) {
        Logger logger = new Logger(MiddlewareHelper.class);

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        logger.d("New refresh token: " + refreshedToken);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String currentToken = preferences.getString(CURRENT_TOKEN, "");
        logger.d("Current token: " + currentToken);

        // If token changed or we are not registered with the middleware register.
        if (refreshedToken != null) {
            register(context, refreshedToken);
        }
    }
}

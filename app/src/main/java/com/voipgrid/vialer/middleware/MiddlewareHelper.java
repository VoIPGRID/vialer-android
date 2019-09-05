package com.voipgrid.vialer.middleware;

import static com.voipgrid.vialer.persistence.Middleware.RegistrationStatus.FAILED;
import static com.voipgrid.vialer.persistence.Middleware.RegistrationStatus.REGISTERED;
import static com.voipgrid.vialer.persistence.Middleware.RegistrationStatus.UNREGISTERED;

import android.content.Context;
import android.os.Build;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.api.Middleware;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.logging.Logger;

import androidx.annotation.NonNull;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handle (un)registration from the middleware in a centralised place.
 */
public class MiddlewareHelper {
    /**
     * Function to set the current registration status with the middleware.
     *
     * @param status Save the registration status with the middleware.
     */
    public static void setRegistrationStatus(
            com.voipgrid.vialer.persistence.Middleware.RegistrationStatus status) {
        User.middleware.setRegistrationStatus(status);
    }

    /**
     * Function to check if the app is currently registered at the middleware.
     *
     * @return boolean if the app is still registered with the middleware.
     */
    public static boolean isRegistered() {
        return User.middleware.getRegistrationStatus() == REGISTERED;
    }

    private static boolean needsUpdate() {
        return User.middleware.getRegistrationStatus() == com.voipgrid.vialer.persistence.Middleware.RegistrationStatus.UPDATE_NEEDED;
    }

    public static boolean needsRegistration() {
        return !isRegistered() || needsUpdate();
    }

    public static void register(final Context context, String token) {
        if (!User.voip.getCanUseSip()) {
            return;
        }

        User.middleware.setLastRegistrationTime(System.currentTimeMillis());

        if (!User.getHasPhoneAccount() || !User.isLoggedIn()) {
            return;
        }

        Middleware api = ServiceGenerator.createRegistrationService(context);

        String sipUserId = User.getPhoneAccount().getAccountId();
        String fullName = User.getVoipgridUser().getFullName();
        String appName = context.getPackageName();
        Call<ResponseBody> call = api.register(
                fullName,
                token,
                sipUserId,
                Build.VERSION.CODENAME,
                Build.VERSION.RELEASE,
                appName,
                (User.remoteLogging.isEnabled() ? User.remoteLogging.getId() : null)
        );
        User.middleware.setCurrentToken(token);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    setRegistrationStatus(REGISTERED);
                } else {
                    setRegistrationStatus(FAILED);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                t.printStackTrace();
                setRegistrationStatus(FAILED);
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

        String token = User.middleware.getCurrentToken();

        if (token == null || token.isEmpty()) {
            logger.d("No token so unregister");
            setRegistrationStatus(FAILED);
            return;
        }

        PhoneAccount phoneAccount = User.getPhoneAccount();

        if (phoneAccount == null) {
            logger.d("No phone account so unregister");
            setRegistrationStatus(
                    com.voipgrid.vialer.persistence.Middleware.RegistrationStatus.FAILED);
            return;
        }

        Middleware api = ServiceGenerator.createRegistrationService(context);

        Call<ResponseBody> call = api.unregister(token, phoneAccount.getAccountId(), context.getPackageName());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    logger.d("unregister successful");
                    setRegistrationStatus(
                            UNREGISTERED);
                } else {
                    logger.d("unregister failed");
                    setRegistrationStatus(FAILED);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                logger.d("unregister failed");
                setRegistrationStatus(FAILED);
            }
        });
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

        String currentToken = User.middleware.getCurrentToken();
        logger.d("Current token: " + currentToken);

        // If token changed or we are not registered with the middleware register.
        if (refreshedToken != null) {
            register(context, refreshedToken);
        }
    }
}

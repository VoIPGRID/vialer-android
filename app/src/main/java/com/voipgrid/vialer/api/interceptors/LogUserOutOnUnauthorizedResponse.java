package com.voipgrid.vialer.api.interceptors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.onboarding.Onboarder;
import com.voipgrid.vialer.onboarding.OnboardingActivity;
import com.voipgrid.vialer.util.AccountHelper;
import com.voipgrid.vialer.util.JsonStorage;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class LogUserOutOnUnauthorizedResponse implements Interceptor {

    private static final int UNAUTHORIZED_HTTP_CODE = 401;

    private Context mContext;
    private Logger mLogger;

    public LogUserOutOnUnauthorizedResponse(Context context) {
        mContext = context;
        mLogger = new Logger(this.getClass());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.code() != UNAUTHORIZED_HTTP_CODE) return response;

        if (userIsCurrentlyOnboarding()) return response;

        mLogger.w("Logged out on 401 API response");

        clearAllCredentials();

        if (requestOriginatedFromAForegroundActivity()) {
            returnUserToLoginScreen();
        }

        return response;
    }

    /**
     * Closes the current activity and takes the user back to the login screen.
     *
     */
    private void returnUserToLoginScreen() {
        Intent intent = new Intent(mContext, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
        ((Activity) mContext).finish();
    }

    /**
     * Checks to see if the context we have originated from an activity, rather than a service.
     *
     * @return
     */
    private boolean requestOriginatedFromAForegroundActivity() {
        return mContext instanceof Activity;
    }

    /**
     * Returns TRUE if the this request originated from the onboarding screen..
     *
     * @return
     */
    private boolean userIsCurrentlyOnboarding() {
        return mContext.getClass().getSimpleName().equals(Onboarder.class.getSimpleName());
    }

    /**
     * Removes all of the users stored credentials and user objects.
     *
     */
    private void clearAllCredentials() {
        new JsonStorage(mContext).clear();
        new AccountHelper(mContext).clearCredentials();
    }
}

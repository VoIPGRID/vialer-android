package com.voipgrid.vialer.api.interceptors;

import com.voipgrid.vialer.Logout;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.logging.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

public class LogUserOutOnUnauthorizedResponse implements Interceptor {

    private static final int UNAUTHORIZED_HTTP_CODE = 401;
    private final Logout logout;

    private Logger mLogger = new Logger(this);

    public LogUserOutOnUnauthorizedResponse(@NonNull Logout logout) {
        this.logout = logout;
    }

    @NotNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.code() != UNAUTHORIZED_HTTP_CODE) return response;

        if (!User.isLoggedIn()) return response;

        mLogger.w("Logged out on 401 API response");

        logout.perform(true, null);

        return response;
    }
}

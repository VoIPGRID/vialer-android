package com.voipgrid.vialer.api.interceptors;

import android.util.Log;

import com.voipgrid.vialer.User;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddAuthorizationCredentialsToRequest implements Interceptor {

    /**
     * The name of the HTTP header that will contain the authorization credentials.
     */
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();

        if (!chain.request().url().toString().contains("call-response")) {
            requestBuilder.header(AUTHORIZATION_HEADER_NAME, createApiTokenAuthHeader());
        }

        return chain.proceed(requestBuilder.build());
    }

    /**
     * Generates the appropriate authorization header to use with an api token.
     *
     * @return The header body as a string
     */
    private String createApiTokenAuthHeader() {
        return "Token " + User.getUsername() + ":" + User.getLoginToken();
    }
}

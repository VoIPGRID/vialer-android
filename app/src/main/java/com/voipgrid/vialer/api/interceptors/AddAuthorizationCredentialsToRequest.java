package com.voipgrid.vialer.api.interceptors;

import android.util.Log;

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

    private String mUsername;
    private String mPassword;
    private String mToken;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();
Log.e("TEST123", chain.request().url().toString());
        if (hasApiToken()) {
            requestBuilder.header(AUTHORIZATION_HEADER_NAME, createApiTokenAuthHeader());
        }
        else if (hasUsernameAndPassword()) {
            requestBuilder.header(AUTHORIZATION_HEADER_NAME, createUsernameAndPasswordAuthHeader());
        }

        return chain.proceed(requestBuilder.build());
    }

    /**
     * Update the credentials on this interceptor, this is so the interceptor can provide
     * the latest credentials without creating a new okhttp client.
     *
     * @param username
     * @param password
     * @param token
     */
    public void setCredentials(String username, String password, String token) {
        mUsername = username;
        mPassword = password;
        mToken = token;
    }

    /**
     * Check if the AccountHelper has an api token.
     *
     * @return TRUE if the AccountHelper has an api token.
     */
    private boolean hasApiToken() {
        return mToken != null && ! mToken.isEmpty();
    }

    /**
     * Check if the AccountHelper has a username/password to use.
     *
     * @return TRUE if the AccountHelper has a stored username/password.
     */
    private boolean hasUsernameAndPassword() {
        return mUsername != null && mPassword != null;
    }

    /**
     * Generates the appropriate authorization header to use with an api token.
     *
     * @return The header body as a string
     */
    private String createApiTokenAuthHeader() {
        return "Token " + mUsername + ":" + mToken;
    }

    /**
     * Generates the appropriate authorization header to use with a username/password.
     *
     * @return The header body as a string
     */
    private String createUsernameAndPasswordAuthHeader() {
        return Credentials.basic(mUsername, mPassword);
    }
}

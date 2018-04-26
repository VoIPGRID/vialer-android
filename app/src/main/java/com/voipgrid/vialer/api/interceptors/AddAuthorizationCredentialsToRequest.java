package com.voipgrid.vialer.api.interceptors;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddAuthorizationCredentialsToRequest implements Interceptor {

    private String mUsername;
    private String mPassword;

    public AddAuthorizationCredentialsToRequest(final String username, final String password) {
        mUsername = username;
        mPassword = password;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();

        if (mUsername != null && mPassword != null) {
            requestBuilder.header("Authorization", Credentials.basic(mUsername, mPassword));
        }

        return chain.proceed(requestBuilder.build());
    }
}

package com.voipgrid.vialer.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.voipgrid.vialer.util.ConnectivityHelper;

import java.io.IOException;
import java.net.Proxy;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Client;
import retrofit.converter.GsonConverter;

/**
 * Project: Vialer
 * Package: com.voipgrid.vialer.api
 * Class  : ServiceGenerator
 * <p/>
 * Created by Eltjo Veninga (eltjo@peperzaken.nl) on 24-07-15 13:33
 * <p/>
 * Copyright (c) 2015 Peperzaken BV. All rights reserved.
 */
public class ServiceGenerator {

    private static ServiceGenerator instance = null;
    private static boolean taken = false;

    private ServiceGenerator() {
    }

    public static ServiceGenerator getInstance() throws PreviousRequestNotFinishedException {
        if (taken) {
            throw new PreviousRequestNotFinishedException("Not ready for new request yet");
        }
        if (instance == null) {
            instance = new ServiceGenerator();
        }
        taken = true;
        return instance;
    }

    public static OkHttpClient getOkHttpClient(Context context, final String username,
                                               final String password) {
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setAuthenticator(new Authenticator() {

            @Override
            public Request authenticate(Proxy proxy, Response response) throws IOException {
                String credential = Credentials.basic(username, password);

                if (credential.equals(response.request().header("Authorization"))) {
                    return null; // If we already failed with these credentials, don't retry.
                }

                return response.request().newBuilder().header("Authorization", credential).build();
            }

            @Override
            public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                return null;
            }
        });

        httpClient.setCache(getCache(context));

        return httpClient;
    }

    public static <S> S createService(final ConnectivityHelper connectivityHelper,
                                      Class<S> serviceClass, String baseUrl, Client client) {
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(baseUrl)
                .setClient(client)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        if (connectivityHelper.hasNetworkConnection()) {
                            int maxAge = 60; // read from cache for 1 minute
                            request.addHeader("Cache-Control", "public, max-age=" + maxAge);
                        } else {
                            int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
                            request.addHeader("Cache-Control",
                                              "public, only-if-cached, max-stale=" + maxStale);
                        }
                    }
                });

        Gson gson = new GsonBuilder()
                .serializeNulls()
                .create();
        builder.setConverter(new GsonConverter(gson));

        RestAdapter adapter = builder.build();

        return adapter.create(serviceClass);
    }

    public static Cache getCache(Context context) {
        return new Cache(context.getCacheDir(), 1024 * 1024 * 10);
    }

    public void release() {
        this.taken = false;
    }
}

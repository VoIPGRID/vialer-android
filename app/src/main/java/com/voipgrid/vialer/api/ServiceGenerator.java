package com.voipgrid.vialer.api;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.interceptors.AddAuthorizationCredentialsToRequest;
import com.voipgrid.vialer.api.interceptors.AddUserAgentToHeader;
import com.voipgrid.vialer.api.interceptors.LogUserOutOnUnauthorizedResponse;
import com.voipgrid.vialer.api.interceptors.ModifyCacheLifetimeBasedOnConnectivity;
import com.voipgrid.vialer.util.AccountHelper;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


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

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static Retrofit.Builder builder = new Retrofit.Builder();

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

    /**
     * Function to create the HttpClient to be used by retrofit for API calls.
     * @param context
     * @param username
     * @param password
     * @return
     */
    private static OkHttpClient getHttpClient(final Context context, final String username,
                                              final String password) {

        httpClient.addInterceptor(new AddAuthorizationCredentialsToRequest(username, password));
        httpClient.addInterceptor(new AddUserAgentToHeader(context));
        httpClient.addInterceptor(new ModifyCacheLifetimeBasedOnConnectivity(context));
        httpClient.addInterceptor(new LogUserOutOnUnauthorizedResponse(context));

        httpClient.cache(getCache(context));

        return httpClient.build();
    }

    public static <S> S createPortalService(final Context context, Class<S> serviceClass,
                                            String username, String password) {
        return createService(context, serviceClass, getVgApiUrl(context),
                username, password);
    }

    /**
     * Create a service for given api class and URL.
     * @param context
     * @param serviceClass
     * @param baseUrl
     * @param <S>
     * @return
     */
    public static <S> S createService(final Context context, Class<S> serviceClass, String baseUrl) {
        return createService(context, serviceClass, baseUrl, null, null);
    }

    /**
     * Create a service for given api class and URL.
     * @param context
     * @param serviceClass
     * @param baseUrl
     * @param username
     * @param password
     * @param <S>
     * @return
     */
    public static <S> S createService(final Context context, Class<S> serviceClass, String baseUrl,
                                      String username, String password) {

        builder.baseUrl(baseUrl)
                .client(getHttpClient(context, username, password))
                .addConverterFactory(
                        GsonConverterFactory.create(new GsonBuilder().serializeNulls().create()));

        Retrofit retrofit = builder.build();

        return retrofit.create(serviceClass);
    }

    public static Api createApiService(Context context) {
        AccountHelper accountHelper = new AccountHelper(context);
        return ServiceGenerator.createService(
                context,
                Api.class,
                context.getString(R.string.api_url),
                accountHelper.getEmail(),
                accountHelper.getPassword()
        );
    }

    private static Cache getCache(Context context) {
        return new Cache(context.getCacheDir(), 1024 * 1024 * 10);
    }

    public static String getVgApiUrl(Context context) {
        return context.getString(R.string.api_url);
    }

    public void release() {
        taken = false;
    }
}

package com.voipgrid.vialer.api;

import android.content.Context;
import android.support.annotation.Nullable;

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
    private static Retrofit sRetrofit;
    private static final AddAuthorizationCredentialsToRequest sAuthorizationInterceptor = new AddAuthorizationCredentialsToRequest();

    private ServiceGenerator() {
    }

    /**
     * Function to create the HttpClient to be used by retrofit for API calls.
     * @param context
     * @return
     */
    private static OkHttpClient getHttpClient(final Context context) {
        httpClient.addInterceptor(sAuthorizationInterceptor);
        httpClient.addInterceptor(new AddUserAgentToHeader(context));
        httpClient.addInterceptor(new ModifyCacheLifetimeBasedOnConnectivity(context));
        httpClient.addInterceptor(new LogUserOutOnUnauthorizedResponse(context));

        httpClient.cache(getCache(context));

        return httpClient.build();
    }

    public static Api createApiService(Context context) {
        AccountHelper accountHelper = new AccountHelper(context);
        return createApiService(context, accountHelper.getEmail(), accountHelper.getPassword(), accountHelper.getApiToken());
    }

    public static Api createApiService(Context context, @Nullable String username, @Nullable String password, @Nullable String token) {
        return ServiceGenerator.createService(context, Api.class, username, password, token);
    }

    public static Registration createRegistrationService(Context context, @Nullable String username, @Nullable String password, @Nullable String token) {
        return ServiceGenerator.createService(context, Registration.class, username, password, token);
    }

    public static Registration createRegistrationService(Context context) {
        AccountHelper accountHelper = new AccountHelper(context);
        return createRegistrationService(context, accountHelper.getEmail(), accountHelper.getPassword(), accountHelper.getApiToken());
    }

    /**
     * Create a service for given api class and URL.
     * @param context
     * @param serviceClass
     * @param <S>
     * @return
     */
    private static <S> S createService(final Context context, Class<S> serviceClass, @Nullable String username, @Nullable String password, @Nullable String token) {
        sAuthorizationInterceptor.setCredentials(username, password, token);

        if (sRetrofit == null) {
            sRetrofit = builder.baseUrl(getVgApiUrl(context))
                    .client(getHttpClient(context))
                    .addConverterFactory(
                            GsonConverterFactory.create(new GsonBuilder().serializeNulls().create())
                    )
                    .build();
        }

        return sRetrofit.create(serviceClass);
    }

    private static Cache getCache(Context context) {
        return new Cache(context.getCacheDir(), 1024 * 1024 * 10);
    }

    private static String getVgApiUrl(Context context) {
        return context.getString(R.string.api_url);
    }
}

package com.voipgrid.vialer.api;

import android.content.Context;
import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.interceptors.AddAuthorizationCredentialsToRequest;
import com.voipgrid.vialer.api.interceptors.AddUserAgentToHeader;
import com.voipgrid.vialer.api.interceptors.LogResponsesToConsole;
import com.voipgrid.vialer.api.interceptors.LogUserOutOnUnauthorizedResponse;
import com.voipgrid.vialer.api.interceptors.ModifyCacheLifetimeBasedOnConnectivity;

import java.util.HashMap;

import javax.inject.Inject;

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
    private static final AddAuthorizationCredentialsToRequest sAuthorizationInterceptor = new AddAuthorizationCredentialsToRequest();

    private static HashMap<String, Retrofit> sRetrofit = new HashMap<>();

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
        httpClient.addInterceptor(new LogUserOutOnUnauthorizedResponse(VialerApplication.get().component().provideLogout()));
        httpClient.addInterceptor(new LogResponsesToConsole());

        httpClient.cache(getCache(context));

        return httpClient.build();
    }

    public static VoipgridApi createApiService(Context context) {
        return ServiceGenerator.createService(context, VoipgridApi.class, getVgApiUrl(context));
    }

    public static Middleware createRegistrationService(Context context) {
        return ServiceGenerator.createService(context, Middleware.class, getRegistrationUrl(context));
    }

    /**
     * Create a service for given api class and URL.
     * @param context
     * @param serviceClass
     * @param <S>
     * @return
     */
    private static <S> S createService(final Context context, Class<S> serviceClass, String url) {
        if (sRetrofit.get(url) == null) {
            sRetrofit.put(url, builder.baseUrl(url)
                    .client(getHttpClient(context))
                    .addConverterFactory(
                            GsonConverterFactory.create(new GsonBuilder().serializeNulls().create())
                    )
                    .build());
        }

        return sRetrofit.get(url).create(serviceClass);
    }

    private static Cache getCache(Context context) {
        return new Cache(context.getCacheDir(), 1024 * 1024 * 10);
    }

    private static String getVgApiUrl(Context context) {
        return context.getString(R.string.api_url);
    }

    private static String getRegistrationUrl(Context context) {
        return context.getString(R.string.registration_url);
    }

}

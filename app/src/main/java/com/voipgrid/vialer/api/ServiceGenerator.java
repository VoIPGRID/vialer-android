package com.voipgrid.vialer.api;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.interceptors.AddAuthorizationCredentialsToRequest;
import com.voipgrid.vialer.api.interceptors.AddUserAgentToHeader;
import com.voipgrid.vialer.api.interceptors.LogResponsesToConsole;
import com.voipgrid.vialer.api.interceptors.LogUserOutOnUnauthorizedResponse;

import androidx.annotation.NonNull;
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

    private static VoipgridApi voipgridApi;
    private static Middleware middleware;

    private ServiceGenerator() {
    }

    @NonNull
    public static VoipgridApi createApiService(@NonNull Context context) {
        if (voipgridApi != null) return voipgridApi;

        return voipgridApi = baseBuilder(context)
                .baseUrl(context.getString(R.string.api_url))
                .build()
                .create(VoipgridApi.class);
    }

    @NonNull
    public static Middleware createRegistrationService(@NonNull Context context) {
        if (middleware != null) return middleware;

        return middleware = baseBuilder(context)
                .baseUrl(context.getString(R.string.registration_url))
                .build()
                .create(Middleware.class);
    }

    /**
     * Create the HTTP client and add all the tasks that must be performed while
     * http requests are occurring.
     *
     */
    private static OkHttpClient createHttpClient(final Context context) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new AddAuthorizationCredentialsToRequest());
        httpClient.addInterceptor(new AddUserAgentToHeader(context));
        httpClient.addInterceptor(new LogUserOutOnUnauthorizedResponse(VialerApplication.get().component().provideLogout()));
        httpClient.addInterceptor(new LogResponsesToConsole());
        return httpClient.build();
    }

    /**
     * Create the base builder, adding the HTTP client and the serialization library.
     *
     * @param context
     * @return
     */
    private static Retrofit.Builder baseBuilder(final Context context) {
        return new Retrofit.Builder()
                .client(createHttpClient(context))
                .addConverterFactory(
                        GsonConverterFactory.create(new GsonBuilder().serializeNulls().create())
                );
    }
}

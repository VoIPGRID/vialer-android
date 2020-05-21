package com.voipgrid.vialer.api;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.interceptors.AddAuthorizationCredentialsToRequest;
import com.voipgrid.vialer.api.interceptors.AddUserAgentToHeader;
import com.voipgrid.vialer.api.interceptors.LogResponsesToConsole;
import com.voipgrid.vialer.api.interceptors.LogUserOutOnUnauthorizedResponse;

import java.util.Collections;

import androidx.annotation.NonNull;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
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
    private static FeedbackApi feedbackApi;

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

    @NonNull
    public static FeedbackApi createFeedbackService(@NonNull Context context) {
        if (feedbackApi != null) return feedbackApi;

        return feedbackApi = baseBuilder(context)
                .baseUrl(context.getString(R.string.feedback_url))
                .build()
                .create(FeedbackApi.class);
    }

    /**
     * Create the HTTP client and add all the tasks that must be performed while
     * http requests are occurring.
     *
     */
    private static OkHttpClient createHttpClient(final Context context) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(createConnectionSpecs()));
        httpClient.addInterceptor(new AddAuthorizationCredentialsToRequest());
        httpClient.addInterceptor(new AddUserAgentToHeader(context));
        httpClient.addInterceptor(new LogUserOutOnUnauthorizedResponse(VialerApplication.get().component().provideLogout()));
        httpClient.addInterceptor(new LogResponsesToConsole());
        return httpClient.build();
    }

    private static ConnectionSpec createConnectionSpecs() {
        return new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                .build();
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

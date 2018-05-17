package com.voipgrid.vialer.api.interceptors;

import android.content.Context;

import com.voipgrid.vialer.util.ConnectivityHelper;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ModifyCacheLifetimeBasedOnConnectivity implements Interceptor {

    private ConnectivityHelper mConnectivityHelper;

    private static final int MAX_AGE = 60;

    private static final int MAX_STALE = 60 * 60 * 24 * 28; // tolerate 4-weeks stale

    public ModifyCacheLifetimeBasedOnConnectivity(Context context) {
        mConnectivityHelper = ConnectivityHelper.get(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = original.
                newBuilder().
                header("Cache-Control", mConnectivityHelper.hasNetworkConnection() ? createMaxAgeHeader() : createMaxStaleHeader()).
                build();

        return chain.proceed(request);
    }

    /**
     * Create the max-age header value.
     *
     * @return
     */
    private String createMaxAgeHeader() {
        return "public, max-age=" + MAX_AGE;
    }

    /**
     * Create the max-stale header value.
     *
     * @return
     */
    private String createMaxStaleHeader() {
        return "public, only-if-cached, max-stale=" + MAX_STALE;
    }
}

package com.voipgrid.vialer.api.interceptors;

import android.content.Context;

import com.voipgrid.vialer.util.UserAgent;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddUserAgentToHeader implements Interceptor {

    private Context mContext;

    public AddUserAgentToHeader(Context context) {
        mContext = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();

        requestBuilder.header("User-Agent", new UserAgent(mContext).generate());

        return chain.proceed(requestBuilder.build());
    }
}

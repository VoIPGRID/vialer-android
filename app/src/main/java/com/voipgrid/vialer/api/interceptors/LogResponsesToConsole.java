package com.voipgrid.vialer.api.interceptors;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LogResponsesToConsole implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        Response response = chain.proceed(request);
        String body = response.body().string();

        try {
            if (shouldLog(request)) {
                JSONObject jsonObject = new JSONObject(body);
                Log.d("HTTP", "Response from " + request.url() + ": " + jsonObject.toString(2));
            }
        } catch (JSONException e) {
            if (body != null && body.length() > 10) {
                Log.e("HTTP", "Unable to format JSON from " + request.url() + ", with body: " + body);
            } else {
                Log.e("HTTP", "Empty response from: " + request.url() +  " (" + response.code() + ")");
            }
        }

        return response.newBuilder()
                .body(ResponseBody.create(response.body().contentType(), body)).build();
    }

    private boolean shouldLog(Request request) {
        return !request.url().toString().contains("cdr");
    }
}

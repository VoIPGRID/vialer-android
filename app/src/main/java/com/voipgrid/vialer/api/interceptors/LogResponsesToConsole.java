package com.voipgrid.vialer.api.interceptors;

import android.util.Log;

import com.google.gson.GsonBuilder;

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
            JSONObject jsonObject = new JSONObject(body);
            Log.d("HTTP", "Response from " + request.url() + ": " + jsonObject.toString(2));
        } catch (JSONException e) {
            Log.e("HTTP", "Unable to format JSON: " + body);
        }

        return response.newBuilder()
                .body(ResponseBody.create(response.body().contentType(), body)).build();
    }
}

package com.voipgrid.vialer;

import androidx.annotation.RequiresApi;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

public class PushReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("pushMessageId", "I am here!");
            // Attempt to extract the "message" property from the payload: {"message":"Hello World!"}
            if (intent.getIntExtra("push-message-id", 0) != 0) {
                int pushMessageId = intent.getIntExtra("push-message-id",0);
                Log.e("testJeremy","testJeremy"+pushMessageId);
                try {
                    run("http://10.13.25.90/call/confirm/pushy");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public static void run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("failedresponse","The response failed" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    Log.e("response","The response is:" +response);
                }
            });
        }
}

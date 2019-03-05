package com.voipgrid.vialer.sip.persistent;

import static com.voipgrid.vialer.fcm.RemoteMessageData.MESSAGE_START_TIME;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipInvite;
import com.voipgrid.vialer.util.NotificationHelper;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pjsip_status_code;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Random;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SipAccount extends Account {

    public SipAccount(AccountConfig config) {
        try {
            create(config);
        } catch (Exception e) {
            Log.e("TEST123", "e", e);
        }
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        super.onRegState(prm);
        Log.e("TEST123", "reg state: "+  prm.getReason() + prm.getRdata().getWholeMsg());
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        super.onIncomingCall(prm);
        SipInvite invite = new SipInvite(prm.getRdata().getWholeMsg());
Log.e("TEST123", "Inc call id;" + invite.getCallId());
Call call = new Call(this);



        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                run("http://10.13.23.180/call/confirm/sip", invite);
            }
        } catch (Throwable e) {
            Log.e("TEST123", "FAILED", e);
        }


        try {
            Thread.sleep(2000);
            CallOpParam callOpParam = new CallOpParam(true);
            callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
            call.hangup(callOpParam);
        } catch (Exception e) {
        }



    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void run(String url, SipInvite sipInvite) throws IOException {
        Request request = new Request.Builder()
                .url(url + "?call_id=" + sipInvite.getCallId() + "&time=" + sipInvite.getTime())
                .build();
Log.e("TEST123", "?call_id=" + sipInvite.getCallId() + "&time=" + sipInvite.getTime());
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {

                Log.e("failedresponse","The response failed" + e.getMessage() + ((ConnectException)e).getLocalizedMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {

                Log.e("response","The response is:" +response);
            }

        });
    }
}

package com.voipgrid.vialer.sip;

import static com.voipgrid.vialer.fcm.RemoteMessageData.MESSAGE_START_TIME;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pjsip_status_code;

import java.io.IOException;
import java.net.ConnectException;

import androidx.annotation.RequiresApi;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Class that reflects a sip account and handles registration.
 */
class SipAccount extends org.pjsip.pjsua2.Account {
    // Callback handler for the onIncomingCall and onRegState events.
    private final AccountStatus mAccountStatus;
    private SipService mSipService;

    /**
     *
     * @param accountConfig configuration to automagically communicate and setup some sort of
     *                      SIP session.
     * @param accountStatus callback object which is used to notify outside world of past events.
     * @throws Exception issue with creating an account.
     */
    public SipAccount(SipService sipService, AccountConfig accountConfig, AccountStatus accountStatus) throws Exception {
        super();
        mAccountStatus = accountStatus;
        mSipService = sipService;
        // Calling create also registers at the server.
        create(accountConfig);
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @param incomingCallParam parameters containing the state of an incoming call.
     */
    @Override
    public void onIncomingCall(OnIncomingCallParam incomingCallParam) {
        Log.e("TEST123", incomingCallParam.getRdata().getWholeMsg());
        SipInvite invite = new SipInvite(incomingCallParam.getRdata().getWholeMsg());
        Log.e("TEST123", "Inc call id;" + invite.getCallId());
        Call call = new Call(this);


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                run("http://10.13.23.180/call/confirm/pushy", invite.getCallId(), invite.getTime());
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
    public static void run(String url, String id, String time) {
        Request request = new Request.Builder()
                .url(url + "?call_id=" + id + "&time=" + time)
                .build();
        Log.e("TEST123", "?call_id=" + id + "&time=" + time);
        new OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {

                Log.e("failedresponse","The response failed" + e.getMessage() + ((ConnectException)e).getLocalizedMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {

                Log.e("response","The response is:" +response);
            }

        });
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @param regStateParam parameters containing the state of this registration.
     */
    @Override
    public void onRegState(OnRegStateParam regStateParam) {
        Log.e("TEST123", "onRegState: " + regStateParam.getReason());
        try {
            AccountInfo info = getInfo();
            if (info.getRegIsActive()) {
                mAccountStatus.onAccountRegistered(this, regStateParam);
            } else {
                mAccountStatus.onAccountUnregistered(this, regStateParam);
            }
        } catch (Exception exception) {
            mAccountStatus.onAccountInvalidState(this, exception);
        }
    }
}

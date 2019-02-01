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

import java.util.Random;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
Log.e("TEST123", "Inc call id;" + prm.getCallId());
Call call = new Call(this);



        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "test channel";
                String description = "test desc";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("111111", name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = VialerApplication.get().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(VialerApplication.get(), "111111")
                    .setContentTitle("Received invite from " + invite.getFrom().name)
                    .setContentText(invite.getFrom().number)
                    .setSmallIcon(R.drawable.ic_blue_phone_icon)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(VialerApplication.get());

            notificationManager.notify(new Random().nextInt(1000) + 1, mBuilder.build());
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
}

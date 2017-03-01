package com.voipgrid.vialer.util;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;

import java.util.Random;

/**
 * NotificationHelper is a class used for displaying Notifications in a uniform way.
 */
public class NotificationHelper {

    public static final String TAG = NotificationHelper.class.getSimpleName();
    private static final int logo = R.drawable.ic_logo;
    public static final int mCallNotifyId = -1;

    // NotificationManager singleton
    private static NotificationHelper mNotificationHelper;
    private static NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private Context mContext;

    private NotificationHelper(Context context) {
        mContext = context;
    }

    public static synchronized NotificationHelper getInstance(Context context) {
        if (mNotificationHelper == null) {
            mNotificationHelper = new NotificationHelper(context);
            mNotificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationHelper;
    }

    public int displayNotificationWithCallActions(String callerId, String number) {
        int notifyId = getNotifyId();

        // Intent when clicking on the notification it self.
        Intent callIntent = new Intent(mContext, mContext.getClass());
        callIntent.setType(CallActivity.TYPE_INCOMING_CALL);
        callIntent.putExtra(CallActivity.CONTACT_NAME, callerId);
        callIntent.putExtra(CallActivity.PHONE_NUMBER, number);
        callIntent.putExtra(TAG, true);

        PendingIntent callPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Decline button intent.
        Intent declineIntent = new Intent(mContext, SipService.class);
        declineIntent.setAction(mContext.getString(R.string.call_incoming_decline));
        declineIntent.setType(SipConstants.CALL_DECLINE_INCOMING_CALL);
        PendingIntent declinePendingIntent = PendingIntent.getService(mContext, 0, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Accept button intent.
        Intent acceptIntent = new Intent(mContext, mContext.getClass());
        acceptIntent.setAction(mContext.getString(R.string.call_incoming_accept));
        acceptIntent.setType(CallActivity.TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL);
        acceptIntent.putExtra(CallActivity.CONTACT_NAME, callerId);
        acceptIntent.putExtra(CallActivity.PHONE_NUMBER, number);

        PendingIntent acceptPendingIntent = PendingIntent.getActivity(mContext, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String notificationTitle = mContext.getString(R.string.call_incoming);
        if (!callerId.isEmpty()) {
            notificationTitle = mContext.getString(R.string.call_incoming_expanded) + " " + callerId;
        }

        mBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(notificationTitle)
                .setContentText(number)
                .setContentIntent(callPendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.ic_call_decline_normal, mContext.getString(R.string.call_incoming_decline), declinePendingIntent)
                .addAction(R.drawable.ic_call_answer_normal, mContext.getString(R.string.call_incoming_accept), acceptPendingIntent);
        mNotificationManager.notify(notifyId, mBuilder.build());
        
        return notifyId;
    }

    public int displayCallProgressNotification(String title, String message, String type, String callerId, String phoneNumber, int notifyId){
        // Cleanup potential notifications that have the same Id.
        removeNotification(notifyId);

        Intent resultIntent = new Intent(mContext, mContext.getClass());
        resultIntent.setType(type);
        resultIntent.putExtra(CallActivity.CONTACT_NAME, callerId);
        resultIntent.putExtra(CallActivity.PHONE_NUMBER, phoneNumber);
        resultIntent.putExtra(TAG, true);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create new notification.
        mBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(logo)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(resultPendingIntent);
        mNotificationManager.notify(notifyId, mBuilder.build());

        // notifyID allows you to update the notification later on.
        return notifyId;
    }

    public void updateNotification(String title, String message, int notifyID) {
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(message);
        mNotificationManager.notify(
                notifyID,
                mBuilder.build()
        );
    }

    public void removeAllNotifications() {
        mNotificationManager.cancelAll();
    }

    public void removeNotification(int notifyID) {
        mNotificationManager.cancel(notifyID);
    }

    private int getNotifyId() {
        return new Random().nextInt();
    }
}

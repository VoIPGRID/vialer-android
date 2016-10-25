package com.voipgrid.vialer.util;


import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;

import java.util.Random;

/**
 * NotificationHelper is a class used for displaying Notifications in a uniform way.
 */
public class NotificationHelper {

    public static final String TAG = NotificationHelper.class.getSimpleName();
    public static final int logo = R.drawable.ic_logo;
    public static final int mCallNotifyId = -1;

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;

    /**
     */
    public int displayNotification(CallActivity callActivity, String title, String message) {
        Random r = new Random();
        int notifyId = r.nextInt();
        return displayNotification(callActivity, title, message, notifyId);
    }

    public int displayNotification(CallActivity callActivity, String title, String message, int notifyId){
        mNotificationManager =
                (NotificationManager) callActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        // cleanup potential notifications that have the same Id.
        removeNotification(notifyId);

        // Create new notification.
        mBuilder = new NotificationCompat.Builder(callActivity)
                .setSmallIcon(logo)
                .setContentTitle(title)
                .setContentText(message);

        Intent resultIntent = new Intent(callActivity, callActivity.getClass());
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.setType(callActivity.mType);
        resultIntent.putExtra(CallActivity.CONTACT_NAME, callActivity.mCallerIdToDisplay);
        resultIntent.putExtra(CallActivity.PHONE_NUMBER, callActivity.mPhoneNumberToDisplay);
        resultIntent.putExtra(TAG, true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(callActivity);
        stackBuilder.addParentStack(callActivity);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager.notify(notifyId, mBuilder.build());
        // notifyID allows you to update the notification later on.
        return notifyId;
    }

    public void updateNotification(Activity activity, String title, String message, int notifyID) {
        NotificationManager notificationManager =
                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(message);
        notificationManager.notify(
                notifyID,
                mBuilder.build());
    }

    public void removeAllNotifications() {
        mNotificationManager.cancelAll();
    }

    public void removeNotification(int notifyID) {
        mNotificationManager.cancel(notifyID);
    }
}

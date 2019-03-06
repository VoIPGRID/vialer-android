package com.voipgrid.vialer.util;



import static android.app.Notification.PRIORITY_MAX;

import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_INCOMING_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.voipgrid.vialer.AccountActivity;
import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.calling.IncomingCallActivity;
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

    private static final String CALLS_NOTIFICATION_CHANNEL_ID = "vialer_calls";

    private static final String CONTACTS_NOTIFICATION_CHANNEL_ID = "vialer_contacts";

    private static final String MEDIA_BUTTON_NOTIFICATION_CHANNEL_ID = "vialer_media_button";

    private static final String VOIP_DISABLED_NOTIFICATION_CHANNEL_ID = "vialer_voip_disabled";

    private static final int CONTACT_SYNC_NOTIFICATION_ID = 1;
    private static final int VOIP_DISABLED_NOTIFICATION_ID = 2;

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
        createCallsNotificationChannel();
        int notifyId = getNotifyId();

        // Intent when clicking on the notification it self.
        Intent callIntent = new Intent(mContext, mContext.getClass());
        callIntent.setType(TYPE_INCOMING_CALL);
        callIntent.putExtra(CONTACT_NAME, callerId);
        callIntent.putExtra(PHONE_NUMBER, number);
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
        Intent acceptIntent = new Intent(mContext, IncomingCallActivity.class);
        acceptIntent.setAction(mContext.getString(R.string.call_incoming_accept));
        acceptIntent.setType(TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL);
        acceptIntent.putExtra(CONTACT_NAME, callerId);
        acceptIntent.putExtra(PHONE_NUMBER, number);

        PendingIntent acceptPendingIntent = PendingIntent.getActivity(mContext, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String notificationTitle = mContext.getString(R.string.call_incoming);
        if (!callerId.isEmpty()) {
            notificationTitle = mContext.getString(R.string.call_incoming_expanded) + " " + callerId;
        }

        mBuilder = new NotificationCompat.Builder(mContext, CALLS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(notificationTitle)
                .setContentText(number)
                .setContentIntent(callPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .addAction(R.drawable.ic_call_decline_normal, mContext.getString(R.string.call_incoming_decline), declinePendingIntent)
                .addAction(R.drawable.ic_call_answer_normal, mContext.getString(R.string.call_incoming_accept), acceptPendingIntent);
        mNotificationManager.notify(notifyId, mBuilder.build());

        return notifyId;
    }

    public int displayCallProgressNotification(String title, String message, String type, String callerId, String phoneNumber, int notifyId){
        createCallsNotificationChannel();
        // Cleanup potential notifications that have the same Id.
        removeNotification(notifyId);

        Intent resultIntent = new Intent(mContext, CallActivity.class);
        resultIntent.setType(type);
        resultIntent.putExtra(CONTACT_NAME, callerId);
        resultIntent.putExtra(PHONE_NUMBER, phoneNumber);
        resultIntent.putExtra(TAG, true);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create new notification.
        mBuilder = new NotificationCompat.Builder(mContext, CALLS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(logo)
                .setOngoing(true)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(resultPendingIntent);
        mNotificationManager.notify(notifyId, mBuilder.build());

        // notifyID allows you to update the notification later on.
        return notifyId;
    }

    public void displayContactsSyncNotification() {
        createContactsNotificationChannel();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, CONTACTS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(mContext.getString(R.string.app_name) + " - " + mContext.getString(R.string.notification_contact_sync_done_title))
                .setContentText(mContext.getString(R.string.notification_contact_sync_done_content))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.notification_contact_sync_done_content)));

        // Create stack for the app to use when clicking the notification.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(mContext, MainActivity.class));

        mBuilder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        // For SDK version greater than 21 we will set the vibration.
        if (Build.VERSION.SDK_INT >= 21) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        }

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {
            mNotificationManager.notify(CONTACT_SYNC_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public Notification createMediaButtonNotification() {
        createMediaButtonChannel();

        return new NotificationCompat.Builder(mContext, MEDIA_BUTTON_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .build();
    }

    public void updateNotification(String title, String message, int notifyID) {
        if (mBuilder == null) {
            return;
        }

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

    private void createCallsNotificationChannel() {
        if(androidVersionDoesNotRequireNotificationChannel()) return;

        NotificationChannel notificationChannel = new NotificationChannel(
                CALLS_NOTIFICATION_CHANNEL_ID,
                mContext.getString(R.string.notification_channel_calls),
                NotificationManager.IMPORTANCE_LOW
        );

        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    private void createContactsNotificationChannel() {
        if(androidVersionDoesNotRequireNotificationChannel()) return;

        NotificationChannel notificationChannel = new NotificationChannel(
                CONTACTS_NOTIFICATION_CHANNEL_ID,
                mContext.getString(R.string.notification_channel_contacts),
                NotificationManager.IMPORTANCE_HIGH
        );

        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    private void createMediaButtonChannel() {
        if(androidVersionDoesNotRequireNotificationChannel()) return;

        NotificationChannel notificationChannel = new NotificationChannel(
                MEDIA_BUTTON_NOTIFICATION_CHANNEL_ID,
                mContext.getString(R.string.notification_channel_media_button),
                NotificationManager.IMPORTANCE_DEFAULT
        );

        notificationChannel.enableVibration(false);

        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    private void createVoipDisabledNotificationChannel() {
        if (androidVersionDoesNotRequireNotificationChannel()) {
            return;
        }

        NotificationChannel notificationChannel = new NotificationChannel(
                VOIP_DISABLED_NOTIFICATION_CHANNEL_ID,
                mContext.getString(R.string.notification_channel_voip_disabled),
                NotificationManager.IMPORTANCE_HIGH
        );

        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    private static boolean androidVersionDoesNotRequireNotificationChannel() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
    }

    private int getNotifyId() {
        return new Random().nextInt();
    }

    public void displayVoipDisabledNotification() {
        createVoipDisabledNotificationChannel();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, VOIP_DISABLED_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentText(mContext.getString(R.string.notification_channel_voip_disabled))
                .setAutoCancel(true)
                .setPriority(PRIORITY_MAX)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.notification_channel_voip_disabled)));

        // Create stack for the app to use when clicking the notification.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(mContext, AccountActivity.class));

        mBuilder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        // For SDK version greater than 21 we will set the vibration.
        if (Build.VERSION.SDK_INT >= 21) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        }

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager != null) {
            mNotificationManager.notify(VOIP_DISABLED_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void removeVoipDisabledNotification() {
        removeNotification(VOIP_DISABLED_NOTIFICATION_ID);
    }
}

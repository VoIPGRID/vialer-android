package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.calling.CallingConstants.TYPE_CONNECTED_CALL;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.util.NotificationHelper;

/**
 * The purpose of this class is to listen to call events and display/update the
 * in-call notifications as appropriate.
 *
 */
public class CallNotifications {

    private final NotificationHelper mNotificationHelper;
    private final Context mContext;

    public CallNotifications(NotificationHelper notificationHelper, Context context) {
        mNotificationHelper = notificationHelper;
        mContext = context;
    }

    public void callWasOpenedFromNotificationButIsNotIncoming(CallNotificationDetail detail) {
        mNotificationHelper.displayCallProgressNotification(
                detail.mCallerInfo,
                mContext.getString(R.string.callnotification_active_call),
                TYPE_CONNECTED_CALL,
                detail.mCallerId,
                detail.mPhoneNumber,
                NotificationHelper.mCallNotifyId
        );
    }

    public void incomingCall(CallNotificationDetail detail) {
        mNotificationHelper.removeAllNotifications();
        mNotificationHelper.displayCallProgressNotification(
                detail.mCallerInfo,
                mContext.getString(R.string.callnotification_incoming_call),
                detail.mType,
                detail.mCallerId,
                detail.mPhoneNumber,
                NotificationHelper.mCallNotifyId
        );
    }

    public void outgoingCall(CallNotificationDetail detail) {
        mNotificationHelper.displayCallProgressNotification(
                detail.mCallerInfo,
                mContext.getString(R.string.callnotification_dialing),
                detail.mType,
                detail.mCallerId,
                detail.mPhoneNumber,
                NotificationHelper.mCallNotifyId
        );
    }

    public void activeCall(CallNotificationDetail detail) {
        mNotificationHelper.removeAllNotifications();
        mNotificationHelper.displayCallProgressNotification(
                detail.mCallerInfo,
                mContext.getString(R.string.callnotification_active_call),
                TYPE_CONNECTED_CALL,
                detail.mCallerId,
                detail.mPhoneNumber,
                NotificationHelper.mCallNotifyId
        );
    }

    public void callScreenIsBeingHiddenOnRingingCall(CallNotificationDetail detail) {
        mNotificationHelper.removeAllNotifications();
        mNotificationHelper.displayNotificationWithCallActions(detail.mCallerId, detail.mPhoneNumber);
    }

    public void removeAll() {
        mNotificationHelper.removeAllNotifications();
    }

    public void update(CallNotificationDetail detail, @StringRes int string) {
        mNotificationHelper.updateNotification(detail.mCallerInfo, mContext.getString(string), NotificationHelper.mCallNotifyId);
    }

    public void acceptedFromNotification(CallNotificationDetail detail) {
        mNotificationHelper.removeNotification(NotificationHelper.mCallNotifyId);
        mNotificationHelper.displayCallProgressNotification(
                detail.mCallerInfo,
                mContext.getString(R.string.callnotification_active_call),
                TYPE_CONNECTED_CALL,
                detail.mCallerId,
                detail.mPhoneNumber,
                NotificationHelper.mCallNotifyId
        );
    }

    public static class CallNotificationDetail {
        private String mCallerInfo;
        private String mCallerId;
        private String mPhoneNumber;
        private final String mType;

        public CallNotificationDetail(String callerInfo, String callerId, String phoneNumber, String type) {
            mCallerInfo = callerInfo;
            mCallerId = callerId;
            mPhoneNumber = phoneNumber;
            mType = type;
        }
    }
}

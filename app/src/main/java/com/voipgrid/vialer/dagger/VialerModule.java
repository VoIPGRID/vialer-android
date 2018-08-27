package com.voipgrid.vialer.dagger;

import android.app.KeyguardManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.calling.CallActivityHelper;
import com.voipgrid.vialer.calling.CallNotifications;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.NotificationHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class VialerModule {
    private final VialerApplication mVialerApplication;

    public VialerModule(VialerApplication vialerApplication) {
        mVialerApplication = vialerApplication;
    }

    @Singleton
    @Provides
    Context provideContext() {
        return mVialerApplication;
    }

    @Singleton
    @Provides
    TelephonyManager provideTelephonyManager() {
        return (TelephonyManager) mVialerApplication.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Singleton
    @Provides
    JsonStorage provideJsonStorage() {
        return new JsonStorage(mVialerApplication);
    }

    @Provides
    ConnectivityManager provideCconnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    ConnectivityHelper provideConnectivityHelper(TelephonyManager telephonyManager, ConnectivityManager connectivityManager) {
        return new ConnectivityHelper(connectivityManager, telephonyManager);
    }

    @Provides
    SystemUser provideSystemUser(JsonStorage jsonStorage) {
        return (SystemUser) jsonStorage.get(SystemUser.class);
    }

    @Provides
    PhoneAccount providePhoneAccount(JsonStorage jsonStorage) {
        return (PhoneAccount) jsonStorage.get(PhoneAccount.class);
    }

    @Provides
    LocalBroadcastManager provideLocalBroadcastManager(Context context) {
        return LocalBroadcastManager.getInstance(context);
    }

    @Provides
    BroadcastReceiverManager provideBroadcastReceiverManager(LocalBroadcastManager localBroadcastManager, Context context) {
        return new BroadcastReceiverManager(localBroadcastManager, context);
    }

    @Provides
    AnalyticsHelper provideAnalyticsHelper() {
        return new AnalyticsHelper(mVialerApplication.getDefaultTracker());
    }

    @Provides
    NotificationHelper provideNotificationHelper(Context context) {
        return NotificationHelper.getInstance(context);
    }

    @Provides
    CallNotifications provideCallNotifications(NotificationHelper notificationHelper, Context context) {
        return new CallNotifications(notificationHelper, context);
    }

    @Provides
    KeyguardManager provideKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Provides
    Contacts provideContacts() {
        return new Contacts();
    }
}

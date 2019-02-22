package com.voipgrid.vialer.dagger;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import com.github.tamir7.contacts.Contact;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.InternalNumbers;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.calling.CallActivityHelper;
import com.voipgrid.vialer.calling.CallNotifications;
import com.voipgrid.vialer.callrecord.CachedContacts;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.callrecord.CallRecordDataSource;
import com.voipgrid.vialer.callrecord.CallRecordDataSourceFactory;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.reachability.ReachabilityReceiver;
import com.voipgrid.vialer.sip.IpSwitchMonitor;
import com.voipgrid.vialer.sip.SipConfig;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.NetworkUtil;
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

    @Provides @Nullable
    PhoneAccount providePhoneAccount(JsonStorage jsonStorage) {
        return (PhoneAccount) jsonStorage.get(PhoneAccount.class);
    }

    @Provides @Nullable
    UserDestination provideUserDestination(JsonStorage jsonStorage) {
        return (UserDestination) jsonStorage.get(UserDestination.class);
    }

    @Provides @Nullable
    InternalNumbers provideInternalNumbers(JsonStorage jsonStorage) {
        return (InternalNumbers) jsonStorage.get(InternalNumbers.class);
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

    @Provides
    Preferences providePreferences(Context context) {
        return new Preferences(context);
    }

    @Provides
    CallActivityHelper provideCallActivityHelper(Contacts contacts) {
        return new CallActivityHelper(contacts);
    }

    @Provides
    IpSwitchMonitor provideIpSwitchMonitor() {
        return new IpSwitchMonitor();
    }

    @Provides
    SipConfig provideSipConfig(Preferences preferences, IpSwitchMonitor ipSwitchMonitor, BroadcastReceiverManager broadcastReceiverManager) {
        return new SipConfig(preferences, ipSwitchMonitor, broadcastReceiverManager);
    }

    @Provides
    SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    ReachabilityReceiver provideReachabilityReceiver(Context context) {
        return new ReachabilityReceiver(context);
    }

    @Provides
    NetworkUtil provideNetworkUtil(Context context) {
        return new NetworkUtil(context);
    }

    @Provides
    Api provideApi(Context context) {
        return ServiceGenerator.createApiService(context);
    }

    @Provides
    CallRecordDataSourceFactory provideCallRecordDataSourceFactory(Api api) {
        return new CallRecordDataSourceFactory(api);
    }

    @Provides
    CallRecordAdapter provideCallRecordAdapter(Context context, CachedContacts cachedContacts) {
        return new CallRecordAdapter(context, cachedContacts);
    }

    @Provides CachedContacts provideCachedContacts(Contacts contacts) {
        return new CachedContacts(contacts);
    }

}

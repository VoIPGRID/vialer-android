package com.voipgrid.vialer.dagger;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.InternalNumbers;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.calling.CallActivityHelper;
import com.voipgrid.vialer.callrecord.CachedContacts;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.callrecord.CallRecordDataSourceFactory;
import com.voipgrid.vialer.callrecord.MissedCalls;
import com.voipgrid.vialer.callrecord.MissedCallsAdapter;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.contacts.PhoneNumberImageGenerator;
import com.voipgrid.vialer.dialer.ToneGenerator;
import com.voipgrid.vialer.reachability.ReachabilityReceiver;
import com.voipgrid.vialer.sip.IpSwitchMonitor;
import com.voipgrid.vialer.sip.NetworkConnectivity;
import com.voipgrid.vialer.sip.SipConfig;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.NetworkUtil;

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

    @Provides SystemUser provideSystemUser(JsonStorage jsonStorage) {
        return (SystemUser) jsonStorage.get(SystemUser.class);
    }

    @Provides @Nullable PhoneAccount providePhoneAccount(JsonStorage jsonStorage) {
        return (PhoneAccount) jsonStorage.get(PhoneAccount.class);
    }

    @Provides @Nullable
    UserDestination provideUserDestination(JsonStorage jsonStorage) {
        return (UserDestination) jsonStorage.get(UserDestination.class);
    }

    @Provides @Nullable InternalNumbers provideInternalNumbers(JsonStorage jsonStorage) {
        return (InternalNumbers) jsonStorage.get(InternalNumbers.class);
    }

    @Provides LocalBroadcastManager provideLocalBroadcastManager(Context context) {
        return LocalBroadcastManager.getInstance(context);
    }

    @Provides
    BroadcastReceiverManager provideBroadcastReceiverManager(LocalBroadcastManager localBroadcastManager, Context context) {
        return new BroadcastReceiverManager(localBroadcastManager, context);
    }

    @Provides AnalyticsHelper provideAnalyticsHelper() {
        return new AnalyticsHelper(mVialerApplication.getDefaultTracker());
    }

    @Provides
    KeyguardManager provideKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Provides Contacts provideContacts() {
        return new Contacts();
    }

    @Provides Preferences providePreferences(Context context) {
        return new Preferences(context);
    }

    @Provides CallActivityHelper provideCallActivityHelper(Contacts contacts) {
        return new CallActivityHelper(contacts);
    }

    @Provides IpSwitchMonitor provideIpSwitchMonitor() {
        return new IpSwitchMonitor();
    }

    @Provides
    SipConfig provideSipConfig(Preferences preferences, IpSwitchMonitor ipSwitchMonitor, BroadcastReceiverManager broadcastReceiverManager) {
        return new SipConfig(preferences, ipSwitchMonitor, broadcastReceiverManager);
    }

    @Provides SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides ReachabilityReceiver provideReachabilityReceiver(Context context) {
        return new ReachabilityReceiver(context);
    }

    @Provides NetworkUtil provideNetworkUtil(Context context) {
        return new NetworkUtil(context);
    }

    @Provides VoipgridApi provideApi(Context context) {
        return ServiceGenerator.createApiService(context);
    }

    @Provides CallRecordDataSourceFactory provideCallRecordDataSourceFactory(VoipgridApi voipgridApi) {
        return new CallRecordDataSourceFactory(voipgridApi);
    }

    @Provides
    CallRecordAdapter provideCallRecordAdapter(CachedContacts cachedContacts) {
        return new CallRecordAdapter(cachedContacts);
    }

    @Provides CachedContacts provideCachedContacts(Contacts contacts) {
        return new CachedContacts(contacts);
    }

    @Provides MissedCalls provideMissedCalls(VoipgridApi voipgridApi) {
        return new MissedCalls(voipgridApi);
    }

    @Provides MissedCallsAdapter provideMissedCallsAdapter(CachedContacts cachedContacts) {
        return new MissedCallsAdapter(cachedContacts);
    }

    @Provides Handler provideHandler() {
        return new Handler();
    }

    @Provides NativeCallManager provideNativeCallManager(TelephonyManager telephonyManager) {
        return new NativeCallManager(telephonyManager);
    }

    @Provides ToneGenerator provideToneGenerator() {
        return new ToneGenerator(AudioManager.STREAM_VOICE_CALL, SipConstants.RINGING_VOLUME);
    }

    @Provides NetworkConnectivity provideNetworkConnectivity() {
        return new NetworkConnectivity();
    }

    @Provides
    PhoneNumberImageGenerator provideNumberImageFinder(Contacts contacts) {
        return new PhoneNumberImageGenerator(contacts);
    }
}

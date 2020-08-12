package com.voipgrid.vialer.dagger;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.voipgrid.contacts.Contacts;
import com.voipgrid.contacts.PhoneNumberImageGenerator;
import com.voipgrid.vialer.Logout;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.PhoneAccountFetcher;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.UserSynchronizer;
import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallScreenWake;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallVibration;
import com.voipgrid.vialer.callrecord.CachedContacts;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.callrecord.database.CallRecordDao;
import com.voipgrid.vialer.callrecord.database.CallRecordsInserter;
import com.voipgrid.vialer.callrecord.importing.CallRecordsFetcher;
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter;
import com.voipgrid.vialer.callrecord.importing.NewCallRecordsImporter;
import com.voipgrid.vialer.middleware.Middleware;
import com.voipgrid.vialer.onboarding.VoipgridLogin;
import com.voipgrid.vialer.util.BatteryOptimizationManager;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ColorHelper;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.HtmlHelper;
import com.voipgrid.vialer.util.NetworkUtil;

import javax.inject.Singleton;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    @Provides
    ConnectivityManager provideCconnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    ConnectivityHelper provideConnectivityHelper(TelephonyManager telephonyManager, ConnectivityManager connectivityManager) {
        return new ConnectivityHelper(connectivityManager, telephonyManager);
    }

    @Provides SystemUser provideSystemUser() {
        return User.getVoipgridUser();
    }

    @Provides @Nullable PhoneAccount providePhoneAccount() {
        return User.getVoipAccount();
    }

    @Provides LocalBroadcastManager provideLocalBroadcastManager(Context context) {
        return LocalBroadcastManager.getInstance(context);
    }

    @Provides
    BroadcastReceiverManager provideBroadcastReceiverManager(LocalBroadcastManager localBroadcastManager, Context context) {
        return new BroadcastReceiverManager(localBroadcastManager, context);
    }

    @Provides
    KeyguardManager provideKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Provides Contacts provideContacts(Context context) {
        return new Contacts(context);
    }

    @Provides SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides NetworkUtil provideNetworkUtil(Context context) {
        return new NetworkUtil(context);
    }

    @Provides VoipgridApi provideApi(Context context) {
        return ServiceGenerator.createApiService(context);
    }

    @Provides
    CallRecordAdapter provideCallRecordAdapter() {
        return new CallRecordAdapter();
    }

    @Provides
    @Singleton
    CachedContacts provideCachedContacts(Contacts contacts) {
        return new CachedContacts(contacts);
    }

    @Provides
    ColorHelper provideColorHelper() {
        return new ColorHelper();
    }

    @Provides
    HtmlHelper provideHtmlHelper() {
        return new HtmlHelper();
    }

    @Provides Handler provideHandler() {
        return new Handler();
    }

    @Provides NativeCallManager provideNativeCallManager(TelephonyManager telephonyManager) {
        return new NativeCallManager(telephonyManager);
    }

    @Provides @Singleton
    PhoneAccountFetcher providePhoneAccountFetcher(VoipgridApi api) {
        return new PhoneAccountFetcher(api);
    }

    @Provides
    PhoneNumberImageGenerator provideNumberImageFinder(Contacts contacts, Context context) {
        return new PhoneNumberImageGenerator(contacts, context);
    }

    @Provides
    android.media.AudioManager provideAudioManager(Context context) {
        return (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Provides
    Vibrator provideVibrator(Context context) {
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Singleton
    @Provides
    IncomingCallVibration provideIncomingCallVibrator(android.media.AudioManager audioManager, Vibrator vibrator) {
        return new IncomingCallVibration(audioManager, vibrator);
    }

    @Singleton
    @Provides
    IncomingCallAlerts incomingCallAlerts(IncomingCallVibration vibration,  IncomingCallScreenWake incomingCallScreenWake) {
        return new IncomingCallAlerts(vibration, incomingCallScreenWake);
    }

    @Singleton
    @Provides
    IncomingCallScreenWake provideIncomingCallScreenWake(PowerManager pm) {
        return new IncomingCallScreenWake(pm);
    }

    @Singleton
    @Provides
    PowerManager providePowerManager(Context context) {
        return (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }


    @Provides
    @Singleton
    BatteryOptimizationManager provideBom(Context context) {
        return new BatteryOptimizationManager(context);
    }

    @Provides
    FirebaseAnalytics provideFirebaseAnalytics(Context context) {
        return FirebaseAnalytics.getInstance(context);
    }

    @Provides
    VoipgridLogin provideVoipgridLogin(Context context) {
        return new VoipgridLogin(context);
    }

    @Provides
    Logout provideLogout(Middleware middleware, SharedPreferences sharedPreferences, ConnectivityHelper connectivityHelper, CallRecordDao database) {
        return new Logout(sharedPreferences, connectivityHelper, database, middleware);
    }

    @Provides
    CallRecordsFetcher provideCallRecordsFetcher() {
        return new CallRecordsFetcher();
    }

    @Provides
    CallRecordDao provideCallRecordDao() {
        return VialerApplication.getDb().callRecordDao();
    }

    @Provides
    CallRecordsInserter provideCallRecordInserter(CallRecordDao db) {
        return new CallRecordsInserter(db);
    }

    @Provides
    NewCallRecordsImporter provideNewCallRecordsImporter(CallRecordsFetcher fetcher, CallRecordsInserter inserter, VoipgridApi api, CallRecordDao db) {
        return new NewCallRecordsImporter(fetcher, inserter, api, db);
    }

    @Provides
    HistoricCallRecordsImporter provideHistoricCallRecordsImporter(CallRecordsFetcher fetcher, CallRecordsInserter inserter, VoipgridApi api, CallRecordDao db) {
        return new HistoricCallRecordsImporter(fetcher, inserter, api);
    }

    @Provides
    VialerApplication provideApplication() {
        return mVialerApplication;
    }

    @Provides
    SecureCalling provideSecureCalling(Context context) {
        return SecureCalling.fromContext(context);
    }

    @Provides
    UserSynchronizer provideUserSync(Middleware middleware, VoipgridApi api, SecureCalling secureCalling) {
        return new UserSynchronizer(api, secureCalling, middleware);
    }

    @Provides
    Middleware provideMiddleware(Context context) {
        return new Middleware(context);
    }
}


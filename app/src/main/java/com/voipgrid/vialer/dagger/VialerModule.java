package com.voipgrid.vialer.dagger;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.voipgrid.vialer.Logout;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.PhoneAccountFetcher;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.models.InternalNumbers;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.PhoneAccounts;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.audio.AudioFocus;
import com.voipgrid.vialer.audio.AudioRouter;
import com.voipgrid.vialer.call.NativeCallManager;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallRinger;
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallVibration;
import com.voipgrid.vialer.calling.CallActivityHelper;
import com.voipgrid.vialer.callrecord.CachedContacts;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.callrecord.CallRecordDataSourceFactory;
import com.voipgrid.vialer.callrecord.MissedCalls;
import com.voipgrid.vialer.callrecord.MissedCallsAdapter;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.contacts.PhoneNumberImageGenerator;
import com.voipgrid.vialer.dialer.ToneGenerator;
import com.voipgrid.vialer.onboarding.VoipgridLogin;
import com.voipgrid.vialer.reachability.ReachabilityReceiver;
import com.voipgrid.vialer.sip.IpSwitchMonitor;
import com.voipgrid.vialer.sip.NetworkConnectivity;
import com.voipgrid.vialer.sip.SipConfig;
import com.voipgrid.vialer.sip.SipConstants;
<<<<<<< HEAD
import com.voipgrid.vialer.util.AccountHelper;
=======
import com.voipgrid.vialer.t9.T9DatabaseHelper;
import com.voipgrid.vialer.t9.T9ViewBinder;
>>>>>>> 0be2efa... Refactored all shared preference interactions to be via a User object, rather than having to import ambiguous classes like JsonStorage. This creates a much more fluent, readable api throughout the code.
import com.voipgrid.vialer.util.BatteryOptimizationManager;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ColorHelper;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.HtmlHelper;
import com.voipgrid.vialer.util.NetworkUtil;
import com.voipgrid.vialer.util.PhoneAccountHelper;

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
        return User.getPhoneAccount();
    }

    @Provides @Nullable InternalNumbers provideInternalNumbers() {
        return User.internal.getInternalNumbers();
    }

    @Provides @Nullable
    PhoneAccounts providePhoneAccounts() {
        return User.internal.getPhoneAccounts();
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

    @Provides Contacts provideContacts() {
        return new Contacts();
    }

    @Provides CallActivityHelper provideCallActivityHelper(Contacts contacts) {
        return new CallActivityHelper(contacts);
    }

    @Provides IpSwitchMonitor provideIpSwitchMonitor() {
        return new IpSwitchMonitor();
    }

    @Provides
    SipConfig provideSipConfig(IpSwitchMonitor ipSwitchMonitor, BroadcastReceiverManager broadcastReceiverManager) {
        return new SipConfig(ipSwitchMonitor, broadcastReceiverManager);
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
    CallRecordAdapter provideCallRecordAdapter() {
        return new CallRecordAdapter();
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

    @Provides ToneGenerator provideToneGenerator() {
        return new ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, SipConstants.RINGING_VOLUME);
    }

    @Provides NetworkConnectivity provideNetworkConnectivity() {
        return new NetworkConnectivity();
    }

    @Provides @Singleton
    PhoneAccountFetcher providePhoneAccountFetcher(VoipgridApi api) {
        return new PhoneAccountFetcher(api);
    }

    @Provides
    PhoneNumberImageGenerator provideNumberImageFinder(Contacts contacts) {
        return new PhoneNumberImageGenerator(contacts);
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

    @Provides
    AudioRouter provideAudioRouter(Context context, android.media.AudioManager androidAudioManager, BroadcastReceiverManager broadcastReceiverManager, AudioFocus audioFocus) {
        return new AudioRouter(context, androidAudioManager, broadcastReceiverManager, audioFocus);
    }

    @Singleton
    @Provides
    IncomingCallAlerts incomingCallAlerts(IncomingCallVibration vibration, IncomingCallRinger ringer) {
        return new IncomingCallAlerts(vibration, ringer);
    }

    @Singleton
    @Provides
    IncomingCallRinger provideRinger(Context context, AudioFocus audioFocus) {
        return new IncomingCallRinger(context, audioFocus);
    }

    @Singleton
    @Provides
    AudioFocus provideAudioFocus(AudioManager audioManager) {
        return new AudioFocus(audioManager);
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
    Logout provideLogout(Context context, SharedPreferences sharedPreferences, ConnectivityHelper connectivityHelper) {
        return new Logout(context, sharedPreferences, connectivityHelper);
    }

    @Provides
    @Singleton
    PhoneAccountHelper providePhoneAccountHelper(Context context) {
        return new PhoneAccountHelper(context);
    }
}

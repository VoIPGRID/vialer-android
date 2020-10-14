package com.voipgrid.vialer.koin

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.PowerManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.voipgrid.vialer.ContactsViewModel
import com.voipgrid.vialer.VialerApplication.Companion.db
import com.voipgrid.vialer.api.PasswordChange
import com.voipgrid.vialer.api.SecureCalling
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.audio.AudioFocus
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallRinger
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallScreenWake
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallVibration
import com.voipgrid.vialer.callrecord.CallRecordViewModel
import com.voipgrid.vialer.middleware.Middleware
import com.voipgrid.vialer.phonelib.Initialiser
import com.voipgrid.vialer.phonelib.SoftPhone
import com.voipgrid.vialer.t9.ContactsSearcher
import com.voipgrid.vialer.util.*
import dagger.Provides
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.openvoipalliance.phonelib.PhoneLib
import javax.inject.Singleton

val appModule = module {

    single<SecureCalling> { SecureCalling.fromContext(androidContext()) }

    single { ServiceGenerator.createApiService(androidContext()) }

    single { ServiceGenerator.createFeedbackService(androidContext()) }

    single { UserSynchronizer(get(), get(), get()) }

    single { BatteryOptimizationManager(androidContext()) }

    single { androidContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    single { androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager }

    single { androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    single { NativeCallManager(get()) }

    single { ServiceGenerator.createRegistrationService(androidContext()) }

    single { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    single { Sim(get()) }

    single { PhoneNumberUtil.getInstance() }

    single { ConnectivityHelper(get(), get()) }

    single { ContactsSearcher() }

    viewModel { ContactsViewModel(get()) }

    viewModel { CallRecordViewModel(get()) }

    single { db.callRecordDao() }

    single { Middleware(androidContext()) }

    single { PasswordChange(get()) }

    single { PhoneLib.getInstance(androidContext()) }

    single { SoftPhone(get(), get()) }

    single { AudioRouter(androidContext(), get(), get(), get()) }

    single { androidContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    single { BroadcastReceiverManager(LocalBroadcastManager.getInstance(androidContext()), androidContext()) }

    single { IncomingCallRinger(androidContext(), get()) }

    single { AudioFocus(get()) }

    single { NetworkUtil(androidContext()) }

    single { IncomingCallAlerts(get(), get(), get()) }

    single { androidContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    single { IncomingCallVibration(get(), get()) }

    single { IncomingCallScreenWake(get()) }

    single { LocalBroadcastManager.getInstance(androidContext()) }

    single { Initialiser(androidContext(), get()) }
}
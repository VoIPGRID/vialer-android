package com.voipgrid.vialer.koin

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.PowerManager
import android.os.Vibrator
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.User
import com.voipgrid.vialer.android.calling.AndroidCallManager
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallRinger
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallScreenWake
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallVibration
import com.voipgrid.vialer.calling.CallActivityHelper
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.dialer.ToneGenerator
import com.voipgrid.vialer.sip.IpSwitchMonitor
import com.voipgrid.vialer.sip.NetworkConnectivity
import com.voipgrid.vialer.sip.SipBroadcaster
import com.voipgrid.vialer.sip.outgoing.OutgoingCallRinger
import com.voipgrid.vialer.sip.pjsip.Pjsip
import com.voipgrid.vialer.sip.pjsip.PjsipConfigurator
import com.voipgrid.vialer.sip.service.SipServiceMonitor
import com.voipgrid.vialer.sip.utils.BusyTone
import com.voipgrid.vialer.sip.utils.ScreenOffReceiver
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.ProximitySensorHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val vialerModule = module {

    single { BroadcastReceiverManager(get(), androidContext()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { NetworkConnectivity() }

    single { IncomingCallAlerts(get(), get(), get()) }

    single { IncomingCallVibration(get(), get()) }

    single { androidContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    single { androidContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    single { IncomingCallRinger(androidContext()) }

    single { IncomingCallScreenWake(get()) }

    single { androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager }

    single { AndroidCallManager(androidContext(), get()) }

    single { androidContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    single { IpSwitchMonitor() }

    single { SipServiceMonitor(get()) }

    single { Handler() }

    single { ScreenOffReceiver() }

    single { NativeCallManager(get()) }

    single { androidContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    single { SipBroadcaster(get()) }

    single { Pjsip(get(), get()) }

    single { User.voipAccount }

    single { PjsipConfigurator(androidContext()) }

    single { BusyTone(get()) }

    single { OutgoingCallRinger(get(), get()) }

    single { ToneGenerator(AudioManager.STREAM_DTMF, 100) }

    single { androidContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

    single { Contacts() }

    single { CallActivityHelper(get()) }
}
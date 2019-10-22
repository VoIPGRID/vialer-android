package com.voipgrid.vialer.koin

import android.content.Context
import android.os.Vibrator
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.MiddlewareApi
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.audio.AudioFocus
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallRinger
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallVibration
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.voip.core.VoipProvider
import com.voipgrid.vialer.voip.middleware.Middleware
import com.voipgrid.vialer.voip.providers.pjsip.PjsipProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val vialerModule = module {

    single { IncomingCallAlerts(get(), get()) }

    single { androidContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    single { IncomingCallVibration(get(), get()) }

    single { IncomingCallRinger(androidContext(), get() )}

    single { BroadcastReceiverManager(get(), get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { User }

    single { Contacts() }
}
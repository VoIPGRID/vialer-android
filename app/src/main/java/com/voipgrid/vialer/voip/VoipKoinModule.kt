package com.voipgrid.vialer.voip

import android.content.Context
import android.media.AudioManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.api.MiddlewareApi
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.audio.AudioFocus
import com.voipgrid.vialer.audio.AudioRouter
import com.voipgrid.vialer.voip.core.VoipProvider
import com.voipgrid.vialer.voip.middleware.Middleware
import com.voipgrid.vialer.voip.providers.pjsip.PjsipProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val voipModule = module {

    single<VoipProvider> { PjsipProvider() }

    single<MiddlewareApi> { ServiceGenerator.createRegistrationService(get()) }

    single { androidContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    single { AudioFocus(get()) }

    single { AudioRouter(get(), get(), get(), get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { Middleware(get(), get(), get()) }
}
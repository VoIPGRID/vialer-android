package nl.voipgrid.vialer_voip

import android.content.Context
import android.media.AudioManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.api.MiddlewareApi
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.audio.AudioFocus
import nl.voipgrid.vialer_voip.android.audio.AndroidAudioRouter
import nl.voipgrid.vialer_voip.android.audio.AudioRouter
import nl.voipgrid.vialer_voip.core.VoipProvider
import nl.voipgrid.vialer_voip.middleware.Middleware
import nl.voipgrid.vialer_voip.providers.pjsip.PjsipProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val voipModule = module {

    single<VoipProvider> { PjsipProvider() }

    single<MiddlewareApi> { ServiceGenerator.createRegistrationService(get()) }

    single <AudioRouter> { AndroidAudioRouter(get(), get(), get(), get()) }

    single { androidContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    single { AudioFocus(get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { Middleware(get(), get(), get()) }
}
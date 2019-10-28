package nl.voipgrid.vialer_voip

import android.content.Context
import android.media.AudioManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.voipgrid.vialer_voip.android.audio.AndroidAudioRouter
import nl.voipgrid.vialer_voip.android.audio.AudioFocus
import nl.voipgrid.vialer_voip.core.audio.AudioRouter
import nl.voipgrid.vialer_voip.core.VoipDriver
import nl.voipgrid.vialer_voip.drivers.pjsip.PjsipDriver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val voipModule = module {

    single<VoipDriver> { PjsipDriver() }


    single <AudioRouter> { AndroidAudioRouter() }

    single { androidContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    single { AudioFocus(get()) }

    single { LocalBroadcastManager.getInstance(get()) }

//    single { Middleware(get(), get(), get()) }
}
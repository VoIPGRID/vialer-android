package com.voipgrid.voip.di

import android.content.Context
import android.telecom.TelecomManager
import com.voipgrid.voip.VoIP
import nl.spindle.phonelib.PhoneLib
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import nl.spindle.phonelib.di.getModules

private val voipModule = module {

    single { com.voipgrid.voip.SoftPhone(androidContext(), get()) }

    single {
        PhoneLib.getInstance(androidContext()).initialise(androidContext())
        PhoneLib.getInstance(androidContext())
    }

    single {
        VoIP(androidContext(), androidContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager, get())
    }
}

val voipModules = listOf(voipModule) + getModules()
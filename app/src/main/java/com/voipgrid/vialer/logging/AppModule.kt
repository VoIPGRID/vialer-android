package com.voipgrid.vialer.logging

import com.voipgrid.vialer.api.SecureCalling
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.util.BatteryOptimizationManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    single<SecureCalling> { SecureCalling.fromContext(androidContext()) }

    single { ServiceGenerator.createApiService(androidContext()) }

    single { UserSynchronizer(get(), androidContext(), get()) }

    single { BatteryOptimizationManager(androidContext()) }
}
package com.voipgrid.vialer.koin

import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.voipgrid.vialer.ContactsViewModel
import com.voipgrid.vialer.VialerApplication.Companion.db
import com.voipgrid.vialer.api.SecureCalling
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.callrecord.CallRecordViewModel
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.t9.ContactsSearcher
import com.voipgrid.vialer.util.BatteryOptimizationManager
import com.voipgrid.vialer.util.ConnectivityHelper
import com.voipgrid.vialer.util.Sim
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<SecureCalling> { SecureCalling.fromContext(androidContext()) }

    single { ServiceGenerator.createApiService(androidContext()) }

    single { UserSynchronizer(get(), androidContext(), get()) }

    single { BatteryOptimizationManager(androidContext()) }

    single { androidContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    single { androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager }

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
}
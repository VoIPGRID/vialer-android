package com.voipgrid.contacts.di

import com.voipgrid.contacts.Contacts
import com.voipgrid.contacts.PhoneNumberImageGenerator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val contactsModule = module {

    single { PhoneNumberImageGenerator(get(), androidContext())}

    single { Contacts(androidContext()) }
}
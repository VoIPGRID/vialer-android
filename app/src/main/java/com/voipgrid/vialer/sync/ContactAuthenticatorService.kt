package com.voipgrid.vialer.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ContactAuthenticatorService : Service() {

    private var mAuthenticator: ContactAuthenticator? = null
    override fun onCreate() {
        mAuthenticator = ContactAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mAuthenticator?.iBinder
    }

}
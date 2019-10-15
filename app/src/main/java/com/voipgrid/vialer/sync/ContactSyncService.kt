package com.voipgrid.vialer.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ContactSyncService : Service() {

    override fun onCreate() {
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = ContactSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter?.syncAdapterBinder
    }

    companion object {
        private var sSyncAdapter: ContactSyncAdapter? = null
        private val sSyncAdapterLock = Any()
    }

}

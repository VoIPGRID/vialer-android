package nl.voipgrid.vialer_voip.android

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

abstract class BindableService<T> : Service() {

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? = binder

    inner class LocalBinder : Binder() {
        fun getService(): T = self()
    }

    abstract fun self(): T
}
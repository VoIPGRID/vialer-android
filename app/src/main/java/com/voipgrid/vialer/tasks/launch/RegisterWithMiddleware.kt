package com.voipgrid.vialer.tasks.launch

import com.google.firebase.iid.FirebaseInstanceId
import com.voipgrid.vialer.VialerApplication
import nl.voipgrid.vialer_voip.middleware.Middleware
import org.koin.core.KoinComponent
import org.koin.core.get

class RegisterWithMiddleware : OnLaunchTask, KoinComponent {

    override fun execute(application: VialerApplication) {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            val middleware: Middleware = get()
            middleware.register(it.token)
        }
    }
}
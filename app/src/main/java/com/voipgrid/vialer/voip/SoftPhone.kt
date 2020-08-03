package com.voipgrid.vialer.voip

import android.util.Log
import com.voipgrid.vialer.User
import com.voipgrid.vialer.logging.Logger
import nl.spindle.phonelib.PhoneLib
import nl.spindle.phonelib.model.RegistrationState
import nl.spindle.phonelib.repository.registration.RegistrationCallback

class SoftPhone(private val phone: PhoneLib) {

    private val logger = Logger(this)

    fun register() {
        if (User.voipAccount == null) {
            logger.e("Unable to register as there is no voip account...")
            return
        }

        User.voipAccount?.let {
            phone.register(it.id, it.password, "sipproxy.voipgrid.nl", "5060", object : RegistrationCallback() {
                override fun stateChanged(registrationState: RegistrationState) {
                    super.stateChanged(registrationState)
                    Log.e("TEST123", "State changed! ${registrationState.name}")
                }
            })
        }
    }
}
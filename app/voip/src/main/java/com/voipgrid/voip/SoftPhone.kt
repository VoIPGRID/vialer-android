package com.voipgrid.voip

import android.annotation.SuppressLint
import android.content.Context
import nl.spindle.phonelib.PhoneLib
import nl.spindle.phonelib.model.Codec
import nl.spindle.phonelib.model.RegistrationState
import nl.spindle.phonelib.model.Session
import nl.spindle.phonelib.repository.initialise.SessionCallback
import nl.spindle.phonelib.repository.registration.RegistrationCallback
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SoftPhone(context: Context, val actions: PhoneLib) {

    lateinit var config: Config
    var call: Call? = null

    init {
        actions.setAudioCodecs(context, setOf(Codec.OPUS))
    }

    suspend fun register(): Boolean = suspendCoroutine { continuation ->

        actions.register(config.username, config.password, config.domain, config.port, object : RegistrationCallback() {
            override fun stateChanged(registrationState: RegistrationState) {
                super.stateChanged(registrationState)
                if (registrationState == RegistrationState.REGISTERED) {
                    continuation.resume(true)
                } else if (registrationState == RegistrationState.FAILED) {
                    continuation.resume(false)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    suspend fun call(number: String): Call {
        register()

        actions.callTo(number)

        return suspendCoroutine { continuation ->
            actions.setSessionCallback(object : SessionCallback() {
                override fun outgoingInit(session: Session) {
                    super.outgoingInit(session)
                    call = Call(session, Call.Direction.INBOUND).also { call ->
                        continuation.resume(call)
                    }
                }

                override fun error(session: Session) {
                    super.error(session)
                    continuation.resumeWithException(Exception())
                }
            })
        }
    }

    suspend fun awaitIncomingCall(): Call {
        register()

        return suspendCoroutine { continuation ->
            actions.setSessionCallback(object : SessionCallback() {
                override fun incomingCall(incomingSession: Session) {
                    super.incomingCall(incomingSession)
                    call = Call(incomingSession, Call.Direction.INBOUND).also { call ->
                        continuation.resume(call)
                    }
                }

                override fun error(session: Session) {
                    super.error(session)
                    continuation.resumeWithException(Exception())
                }
            })
        }
    }


    companion object {
        lateinit var config: Config
    }
}
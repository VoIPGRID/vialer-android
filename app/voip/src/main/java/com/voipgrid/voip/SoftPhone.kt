package com.voipgrid.voip

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import nl.spindle.phonelib.PhoneLib
import nl.spindle.phonelib.model.Codec
import nl.spindle.phonelib.model.RegistrationState
import nl.spindle.phonelib.model.Session
import nl.spindle.phonelib.repository.initialise.SessionCallback
import nl.spindle.phonelib.repository.registration.RegistrationCallback
import kotlin.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SoftPhone(context: Context, val actions: PhoneLib) {

    var sessionUpdate: (() -> Unit)? = null
    var call: Call? = null

    private var continuation: Continuation<Call>? = null

    private val sesssionReceiver: SessionCallback = SessionReceiver()

    init {
        actions.setAudioCodecs(context, setOf(Codec.OPUS))
        actions.setSessionCallback(sesssionReceiver)
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

        return suspendCoroutine { continuation ->
            this.continuation = continuation
            actions.callTo(number)
        }
    }

    suspend fun awaitIncomingCall(): Call {
        return suspendCoroutine { continuation ->
            this.continuation = continuation
        }
    }

    fun setMuteMicrophone(mute: Boolean) = actions.setMuteMicrophone(mute)

    fun isMicrophoneMuted() = actions.isMicrophoneMuted()


    companion object {
        lateinit var config: Config
    }

    inner class SessionReceiver: nl.spindle.phonelib.repository.initialise.SessionCallback() {

        override fun incomingCall(incomingSession: Session) {
            super.incomingCall(incomingSession)
Log.e("TEST123", "SessionRec: incomingCall")
            call = Call(incomingSession, Call.Direction.INBOUND).also { call ->
                continuation?.resume(call)
            }
        }

        override fun outgoingInit(session: Session) {
            super.outgoingInit(session)
            Log.e("TEST123", "SessionRec: outgoingInit")
            call = Call(session, Call.Direction.OUTBOUND).also { call ->
                continuation?.resume(call)
            }
        }

        override fun sessionConnected(session: Session) {
            super.sessionConnected(session)
            Log.e("TEST123", "SessionRec: sessionConnected")
        }

        override fun sessionEnded(session: Session) {
            super.sessionEnded(session)
            Log.e("TEST123", "SessionRec: sessionEnded")
            call?.connection?.let {
                call = null
                it.onDisconnect()
            }
        }

        override fun sessionReleased(session: Session) {
            super.sessionReleased(session)
            Log.e("TEST123", "SessionRec: sessionReleased")
            call?.connection?.let {
                call = null
                it.onDisconnect()
            }
        }

        override fun sessionUpdated(session: Session) {
            super.sessionUpdated(session)
            Log.e("TEST123", "SessionRec: sessionUpdated")
            sessionUpdate?.invoke()
        }

        override fun error(session: Session) {
            super.error(session)
            Log.e("TEST123", "SessionRec: error")
            call?.connection?.onDisconnect()
            continuation?.resumeWithException(Exception("Something failed when setting up the call..."))
        }
    }
}
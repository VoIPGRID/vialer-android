package com.voipgrid.voip

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.voipgrid.voip.android.VoIPService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.spindle.phonelib.BuildConfig

class VoIP(private val context: Context, private val telecomManager: TelecomManager, private val softPhone: SoftPhone) {

    private val handle: PhoneAccountHandle by lazy {
        PhoneAccountHandle(ComponentName(context, VoIPService::class.java), PHONE_ACCOUNT_HANDLE_ID)
    }

    private val phoneAccount: PhoneAccount by lazy {
        PhoneAccount.builder(handle, "Vialer").setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED).build()
    }

    @SuppressLint("MissingPermission")
    fun placeCall(number: String) {

        telecomManager.registerPhoneAccount(phoneAccount)

        telecomManager.placeCall(
                Uri.fromParts("", number, null),
                Bundle().apply {
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, false)
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                    putBoolean(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, false)
                }
        )
    }

    fun addNewIncomingCall(onRegister: (() -> Unit)?) {
        telecomManager.registerPhoneAccount(phoneAccount)

        GlobalScope.launch(Main) {
            softPhone.register()

            onRegister?.invoke()

            softPhone.awaitIncomingCall()

            telecomManager.addNewIncomingCall(handle, Bundle())
        }

    }

    companion object {
        private const val PHONE_ACCOUNT_HANDLE_ID = BuildConfig.LIBRARY_PACKAGE_NAME
    }
}
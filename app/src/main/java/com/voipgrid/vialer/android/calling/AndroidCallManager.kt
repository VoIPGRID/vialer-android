package com.voipgrid.vialer.android.calling

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import com.voipgrid.vialer.BuildConfig

class AndroidCallManager(private val context: Context, private val telecomManager: TelecomManager) {

    private val phoneAccountHandle: PhoneAccountHandle by lazy {
        PhoneAccountHandle(ComponentName(context, AndroidCallService::class.java), PHONE_ACCOUNT_HANDLE_ID)
    }

    private val phoneAccount: PhoneAccount by lazy {
        PhoneAccount.builder(phoneAccountHandle, "Vialer").setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED).build()
    }

    /**
     * Place a call via the android call system.
     *
     */
    fun call(number: String) {
        val extras = Bundle().apply {
            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
            putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        }

        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("We do not have a call permission")
        }

        telecomManager.apply {
            registerPhoneAccount(phoneAccount)
            placeCall(Uri.fromParts(PhoneAccount.SCHEME_SIP, number, null), extras)
        }
    }

    /**
     * Make the android call system aware that there is an incoming call.
     *
     */
    fun incomingCall() {
        val extras = Bundle().apply {
            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
            putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        }

        telecomManager.apply {
            registerPhoneAccount(phoneAccount)
            addNewIncomingCall(phoneAccountHandle, extras)
        }
    }

    companion object {
        private const val PHONE_ACCOUNT_HANDLE_ID = BuildConfig.APPLICATION_ID
    }
}
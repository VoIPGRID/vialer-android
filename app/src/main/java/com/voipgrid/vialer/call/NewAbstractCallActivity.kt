package com.voipgrid.vialer.call

import android.graphics.Bitmap
import com.voipgrid.vialer.R
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.util.LoginRequiredActivity
import kotlinx.android.synthetic.main.activity_call.*
import org.koin.android.ext.android.inject

abstract class NewAbstractCallActivity : LoginRequiredActivity() {

    private val contacts: Contacts by inject()

    protected fun renderCallerInformation() {
        val call = voip?.getCurrentCall() ?: return

        caller_title.text = if (call.metaData.callerId.isNotBlank()) call.metaData.callerId else call.metaData.number
        caller_subtitle.text = if (call.metaData.callerId.isNotBlank()) call.metaData.number else ""
        caller_image.setImageResource(R.drawable.no_user)

        contacts.getContactImageByPhoneNumber(call.metaData.number)?.let {
            caller_image.setImageBitmap(it)
        }

        contacts.getContactNameByPhoneNumber(call.metaData.number)?.let {
            caller_title.text = it
            caller_subtitle.text = call.metaData.number
        }
    }
}
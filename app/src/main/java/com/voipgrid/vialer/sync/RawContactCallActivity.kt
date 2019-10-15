package com.voipgrid.vialer.sync

import android.os.Bundle
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.SipUri
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.util.PhoneNumberUtils

class RawContactCallActivity : LoginRequiredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contactName = "REEEEEEEE"
        val number = "521"
        val uri = SipUri.sipAddressUri(
                this,
                PhoneNumberUtils.format(number)
        )
        val bundle = Bundle()
        bundle.putString(SipConstants.EXTRA_PHONE_NUMBER, number)
        bundle.putString(SipConstants.EXTRA_CONTACT_NAME, contactName)
        SipService.createSipServiceAction(SipService.Actions.HANDLE_OUTGOING_CALL, uri, bundle)
    }

}
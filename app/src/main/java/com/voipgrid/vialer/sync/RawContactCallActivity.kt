package com.voipgrid.vialer.sync

import android.os.Bundle
import android.provider.ContactsContract
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.DialHelper
import com.voipgrid.vialer.util.LoginRequiredActivity

class RawContactCallActivity : LoginRequiredActivity() {

    companion object {
        private const val THIRD_PARTY_ACTION = "third_party_action"
        private const val CONTENT_SCHEME = "content"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras ?: return
        val action = extras.getString(THIRD_PARTY_ACTION)
        if (action == null || action != getString(R.string.call_mime_type)) {
            return
        }
        val data = intent.data ?: return
        if (data.scheme != CONTENT_SCHEME) {
            return
        }
        val cursor = contentResolver.query(data, null, null, null, null) ?: return
        if (!cursor.moveToNext()) {
            cursor.close()
            return
        }
        val number = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.Data.DATA1))
        cursor.close()
        DialHelper.fromActivity(this).callNumber(number, "REEEEE")
    }

}
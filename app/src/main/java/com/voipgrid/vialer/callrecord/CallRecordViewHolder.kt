package com.voipgrid.vialer.callrecord

import android.app.Activity
import android.graphics.Bitmap
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.PhoneAccountFetcher
import com.voipgrid.vialer.api.models.CallRecord
import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.util.DialHelper
import com.voipgrid.vialer.util.IconHelper
import com.voipgrid.vialer.util.TimeUtils
import kotlinx.android.synthetic.main.list_item_call_record.view.*
import javax.inject.Inject

class CallRecordViewHolder(private val view : View) : RecyclerView.ViewHolder(view), View.OnClickListener {

    private lateinit var callRecord: CallRecord
    private lateinit var activity: Activity
    private var callAlreadySetup = false

    @Inject lateinit var phoneAccountFetcher : PhoneAccountFetcher
    @Inject lateinit var contacts: CachedContacts

    init {
        VialerApplication.get().component().inject(this)
        view.call_button.setOnClickListener(this)
    }

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    /**
     * Update this view based on the call record.
     *
     * @param callRecord
     */
    fun update(callRecord: CallRecord) {
        this.callRecord = callRecord
        val number = callRecord.thirdPartyNumber
        val contact = contacts.getContact(number)
        view.icon.setImageBitmap(getContactImage(number, contact))
        setNumberAndCallButtonVisibility(callRecord, contact)
        view.subtitle.setCompoundDrawablesWithIntrinsicBounds(getIcon(callRecord), 0, 0, 0)
        view.subtitle.text = createContactInformationString()

        if (callRecord.wasAnsweredElsewhere()) {
            view.subtitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_call_record_answered_elsewhere, 0, 0, 0)
            phoneAccountFetcher.fetch(callRecord.destinationAccount, PhoneAccountFetcherCallback())
        }
    }

    private fun createContactInformationString(suffix: String = ""): String {
        val result = DateUtils.getRelativeDateTimeString(
                activity,
                TimeUtils.convertToSystemTime(callRecord.callDate),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.YEAR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_TIME
        ).toString()

        return if (suffix.isEmpty()) result else "$result - $suffix"
    }

    private fun setNumberAndCallButtonVisibility(callRecord: CallRecord, contact: Contact?) {
        if (callRecord.isAnonymous) {
            view.title.text = activity.getString(R.string.supressed_number)
            view.call_button.visibility = View.GONE
            return
        }

        if (contact != null) {
            view.title.text = contact.displayName
            view.call_button.visibility = View.VISIBLE
            return
        }

        view.title.text = callRecord.thirdPartyNumber
        view.call_button.visibility = View.VISIBLE
    }

    /**
     * Attempt to find the contact's image, falling back to a generic icon if
     * none is available.
     *
     * @param number
     * @param contact
     * @return
     */
    private fun getContactImage(number: String, contact: Contact?): Bitmap {
        if (contact == null) {
            return IconHelper.getCallerIconBitmap("", number, 0)
        }

        val contactImage = contacts.getContactImage(number)

        return contactImage
                ?: IconHelper.getCallerIconBitmap(contact.displayName.substring(0, 1), number, 0)
    }

    /**
     * Determine the correct call icon to display.
     *
     * @param callRecord
     * @return
     */
    private fun getIcon(callRecord: CallRecord): Int {
        if (callRecord.direction == CallRecord.DIRECTION_OUTBOUND) {
            return R.drawable.ic_call_record_outgoing_call
        }

        return if (callRecord.duration == 0) R.drawable.ic_call_record_missed_call else R.drawable.ic_call_record_incoming_call
    }

    override fun onClick(view: View) {
        if (callRecord.thirdPartyNumber != null && !callAlreadySetup) {
            callAlreadySetup = true
            DialHelper.fromActivity(activity).callNumber(callRecord.thirdPartyNumber, "")
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(
                    DialerActivity.LAST_DIALED, callRecord.thirdPartyNumber).apply()
        }

        callAlreadySetup = false
    }

    inner class PhoneAccountFetcherCallback : PhoneAccountFetcher.Callback {
        override fun onSuccess(phoneAccount: PhoneAccount) {
            view.subtitle.text = createContactInformationString(createAnsweredElsewhereString(phoneAccount))
        }

        private fun createAnsweredElsewhereString(phoneAccount: PhoneAccount) : String {
            return activity.getString(
                    R.string.call_records_answered_elsewhere,
                    phoneAccount.description,
                    phoneAccount.number
            )
        }
    }
}
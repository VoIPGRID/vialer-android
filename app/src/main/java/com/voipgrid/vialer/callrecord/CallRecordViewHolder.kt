package com.voipgrid.vialer.callrecord

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateUtils
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.recyclerview.widget.RecyclerView
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.PhoneAccountFetcher
import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import com.voipgrid.contacts.Contacts
import com.voipgrid.vialer.util.IconHelper
import com.voipgrid.vialer.util.TimeUtils
import kotlinx.android.synthetic.main.list_item_call_record.view.*
import javax.inject.Inject


class CallRecordViewHolder(private val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

    private lateinit var callRecord: CallRecordEntity
    private lateinit var activity: Activity
    private var callAlreadySetup = false

    @Inject lateinit var phoneAccountFetcher: PhoneAccountFetcher
    @Inject lateinit var contacts: CachedContacts

    init {
        VialerApplication.get().component().inject(this)
        view.call_button.setOnClickListener(this)
        view.setOnClickListener {
            startCall()
        }
    }

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    /**
     * Update this view based on the call record.
     *
     * @param callRecord
     */
    fun bindTo(callRecord: CallRecordEntity) {
        this.callRecord = callRecord
        val number = callRecord.thirdPartyNumber
        val contact = contacts.getContact(number)
        view.icon.setImageBitmap(getContactImage(number, contact))
        setNumberAndCallButtonVisibility(callRecord, contact)
        view.subtitle.setCompoundDrawablesWithIntrinsicBounds(getIcon(callRecord), 0, 0, 0)
        view.subtitle.text = createContactInformationString()

        if (callRecord.wasAnsweredElsewhere) {
            view.subtitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_call_record_answered_elsewhere, 0, 0, 0)
            callRecord.destinationAccount?.let {
                phoneAccountFetcher.fetch(it, PhoneAccountFetcherCallback())
            }
        }
    }

    private fun createContactInformationString(suffix: String = ""): String {
        val result = DateUtils.getRelativeDateTimeString(
                activity,
                TimeUtils.convertToSystemTime(callRecord.callTime),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.YEAR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_TIME
        ).toString()

        return if (suffix.isEmpty()) result else "$result - $suffix"
    }

    private fun setNumberAndCallButtonVisibility(callRecord: CallRecordEntity, contact: Contact?) {
        if (callRecord.isAnonymous()) {
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
    private fun getIcon(callRecord: CallRecordEntity): Int {
        if (callRecord.direction == CallRecordEntity.DIRECTION.OUTBOUND) {
            return R.drawable.ic_call_record_outgoing_call
        }

        return if (callRecord.duration == 0) R.drawable.ic_call_record_missed_call else R.drawable.ic_call_record_incoming_call
    }

    override fun onClick(view: View) {
        val popup = PopupMenu(activity, view.call_button)
        popup.menuInflater.inflate(R.menu.menu_recent_call, popup.menu)
        if (com.voipgrid.contacts.Contacts(VialerApplication.get()).getContactByPhoneNumber(callRecord.thirdPartyNumber) != null) {
            popup.menu.removeItem(R.id.add_to_contacts)
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.start_call -> startCall()
                R.id.add_to_contacts -> addToContacts()
                R.id.copy_number -> copyNumber()
            }
            true
        }
        popup.show()
    }

    private fun startCall() {
        throw NotImplementedError("not implemented")
//        if (!callAlreadySetup) {
//            callAlreadySetup = true
//            DialHelper.fromActivity(activity).callNumber(callRecord.thirdPartyNumber, "")
//            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(
//                    DialerActivity.LAST_DIALED, callRecord.thirdPartyNumber).apply()
//        }

        //callAlreadySetup = false
    }

    private fun addToContacts() {
        val contactIntent = Intent(ContactsContract.Intents.Insert.ACTION)
        val displayName = contacts.getContact(callRecord.thirdPartyNumber)?.displayName
        contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE

        contactIntent
                .putExtra(ContactsContract.Intents.Insert.NAME, displayName)
                .putExtra(ContactsContract.Intents.Insert.PHONE, callRecord.thirdPartyNumber)

        startActivityForResult(activity, contactIntent, 1, Bundle())
    }

    private fun copyNumber() {
        val toCopy = callRecord.thirdPartyNumber
        val clipboard = activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("number", toCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(activity, activity.getString(R.string.number_copied, callRecord.thirdPartyNumber), Toast.LENGTH_SHORT).show()
    }

    inner class PhoneAccountFetcherCallback : PhoneAccountFetcher.Callback {
        override fun onSuccess(phoneAccount: PhoneAccount) {
            view.subtitle.text = createContactInformationString(createAnsweredElsewhereString(phoneAccount))
        }

        private fun createAnsweredElsewhereString(phoneAccount: PhoneAccount): String {
            return activity.getString(
                    R.string.call_records_answered_elsewhere,
                    phoneAccount.description,
                    phoneAccount.number
            )
        }
    }
}
package com.voipgrid.vialer.contacts.dialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.Contacts
import com.github.tamir7.contacts.PhoneNumber
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import com.voipgrid.vialer.callrecord.database.CallRecordEntity.DIRECTION.INBOUND
import com.voipgrid.vialer.contacts.ContactsFragment
import com.voipgrid.vialer.contacts.initials
import com.voipgrid.vialer.findLookupId
import com.voipgrid.vialer.util.IconHelper
import com.voipgrid.vialer.util.TimeUtils.convertToSystemTime
import kotlinx.android.synthetic.main.fragment_contact.*
import kotlinx.android.synthetic.main.list_item_contact_call_record.view.*
import kotlinx.android.synthetic.main.list_item_phone_number.view.*
import kotlinx.android.synthetic.main.list_item_phone_number.view.divider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.text.SimpleDateFormat
import java.util.*

class ContactDialog(private val contactId: Long) : DialogFragment(), KoinComponent {

    private val db: CallRecordDao by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contact, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadContact()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        loadContact()
    }

    private fun loadContact() {
        val contact = Contacts.getQuery().whereEqualTo(Contact.Field.ContactId, contactId).find().first()

        name.text = contact.displayName
        setIcon(contact)
        edit_contact.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(ContactsContract.Contacts.getLookupUri(contact.id, contact.findLookupId()), ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                putExtra("finishActivityOnSaveCompleted", true)
            }, 1)
        }

        (phone_number_list as RecyclerView).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = PhoneNumberAdapter(contact, contact.phoneNumbers)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val calls = withContext(Dispatchers.IO) {
                db.findRecentCallsFor(contact.phoneNumbers.map {
                    it.number
                }.toTypedArray())
            }

            if (calls.isNotEmpty()) {
                (recent_calls_list as RecyclerView).apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(activity)
                    adapter = RecentCallsAdapter(calls)
                }
                recent_calls.visibility = View.VISIBLE
            } else {
                recent_calls.visibility = View.GONE
            }
        }
    }

    private fun setIcon(contact: Contact) {
        if (contact.photoUri != null && contact.photoUri.isNotBlank()) {
            image.setImageURI(Uri.parse(contact.photoUri))
            return
        }

        image.setImageBitmap(
                IconHelper.getCallerIconBitmap(contact.displayName.initials(), contact.phoneNumbers[0].toString(), 0)
        )
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
        }
    }

    class PhoneNumberAdapter(private val contact: Contact, private val dataset: List<PhoneNumber>) : RecyclerView.Adapter<PhoneNumberAdapter.ViewHolder>() {

        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.list_item_phone_number, parent, false)
            )
        }

        override fun getItemCount() = dataset.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val phoneNumber = dataset[position]
            holder.view.phone_number.text = phoneNumber.number
            holder.view.phone_number_type.text = phoneNumber.type.toString().toLowerCase().capitalize()
            holder.view.divider.visibility = if (dataset.size != (position + 1)) View.VISIBLE else View.GONE
            holder.view.phone_number_container.setOnClickListener {
                ContactsFragment.contactNumberWasClickedCallback?.invoke(contact, phoneNumber.number)
            }
        }
    }

    class RecentCallsAdapter(private val dataset: List<CallRecordEntity>) : RecyclerView.Adapter<RecentCallsAdapter.ViewHolder>() {

        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.list_item_contact_call_record, parent, false)
            )
        }

        override fun getItemCount() = dataset.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val callRecord = dataset[position]
            holder.view.call_record_phone_number.text = callRecord.thirdPartyNumber
            holder.view.call_record_duration.text = if (!callRecord.wasMissed) {
                PeriodFormatterBuilder()
                        .printZeroRarelyLast()
                        .appendHours()
                        .appendSuffix("h")
                        .appendMinutes()
                        .appendSuffix("m")
                        .appendSeconds()
                        .appendSuffix("s")
                        .toFormatter()
                        .print(Period(callRecord.duration.toLong() * 1000))
            } else {
                VialerApplication.get().getString(R.string.contact_call_records_missed)
            }

            holder.view.divider.visibility = if (dataset.size != (position + 1)) View.VISIBLE else View.GONE
            holder.view.call_record_date.text = SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(convertToSystemTime(callRecord.callTime)))
            holder.view.call_record_direction_image.setImageResource(
                    when {
                        callRecord.wasMissed -> R.drawable.ic_call_record_missed_call
                        callRecord.direction == INBOUND -> R.drawable.ic_call_record_incoming_call
                        else -> R.drawable.ic_call_record_outgoing_call
                    }
            )
            holder.view.call_record_direction.text = VialerApplication.get().getString(
                    if (callRecord.direction == INBOUND) R.string.call_incoming_short else R.string.call_outgoing_short
            )
        }
    }
}
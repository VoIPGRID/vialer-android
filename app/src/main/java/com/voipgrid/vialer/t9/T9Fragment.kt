package com.voipgrid.vialer.t9

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import androidx.appcompat.app.AlertDialog
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.PhoneNumber
import com.voipgrid.vialer.R
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.permissions.ContactsPermission
import kotlinx.android.synthetic.main.fragment_t9_search.*
import kotlinx.android.synthetic.main.list_item_contact.view.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class T9Fragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var job: Job

    private val helper = T9HelperFragment()
    private val contactsSearcher = ContactsSearcher()
    private val adapter = T9RecyclerAdapter()
    var listener: Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_t9_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction().add(R.id.helper, helper).commit()
        job = Job()

        list_view.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            adapter = this@T9Fragment.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)
                    if (state == SCROLL_STATE_TOUCH_SCROLL) {
                        listener?.onExpandRequested()
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()

        if (ContactsPermission.hasPermission(activity)) {
            launch { contactsSearcher.refresh() }
        }
    }

    override fun onDestroyView() {
        job.cancel()
        super.onDestroyView()
    }

    /**
     * Updates the UI based on the current state.
     *
     */
    private fun updateUi(query: String = "") {
        if (!ContactsPermission.hasPermission(activity)) {
            list_view.visibility = View.GONE
            contacts_empty.visibility = View.GONE
            helper.hide()
            permission_contact_description.text = getString(R.string.permission_contact_description, getString(R.string.app_name))
            no_contact_permission_warning.visibility = View.VISIBLE
            give_contact_permission_button.setOnClickListener {
                startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity?.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }, 1)
            }
            return
        }

        no_contact_permission_warning.visibility = View.GONE

        if (query.isEmpty()) {
            helper.show()
            list_view.visibility = View.GONE
            contacts_empty.visibility = View.GONE
            return
        }

        helper.hide()

        if (adapter.currentList.isNotEmpty()) {
            contacts_empty.visibility = View.GONE
            list_view.visibility = View.VISIBLE
        } else {
            list_view.visibility = View.GONE
            contacts_empty.visibility = View.VISIBLE
        }
    }

    fun search(query: String) {
        coroutineContext.cancelChildren()

        launch(Dispatchers.Main) {
            val t9query = T9.convertT9QueryToRegexQuery(query)
            val contacts = contactsSearcher.t9(t9query)
            adapter.query = t9query
            adapter.submitList(contacts)
            updateUi(query)
        }
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }

    /**
     * Called when a user clicks on a contact in the list.
     *
     */
    fun contactWasClicked(contact: Contact) {
        if (contact.phoneNumbers.size <= 1) {
            listener?.onContactSelected(contact.phoneNumbers[0].number, contact.displayName)
            return
        }

        val numbers = contact.phoneNumbers.fold(mutableListOf()) { list: MutableList<String>, phoneNumber: PhoneNumber ->
            list.add("${phoneNumber.number} (${phoneNumber.type.toString().toLowerCase().capitalize()})")
            list
        }.toTypedArray()

        activity?.let {
            AlertDialog.Builder(it)
                    .setTitle(contact.displayName)
                    .setItems(numbers) { _: DialogInterface, position: Int -> listener?.onContactSelected(contact.phoneNumbers[position].number, contact.displayName) }
                    .show()
        }
    }

    interface Listener {
        fun onExpandRequested()

        fun onContactSelected(number: String, name: String)
    }

    private inner class T9RecyclerAdapter : ListAdapter<Contact, ContactViewHolder>(DiffCallback()) {
        var query: Regex? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            return ContactViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.list_item_contact, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            holder.bind(getItem(position), query ?: Regex(""))
        }

        override fun submitList(list: List<Contact>?) {
            super.submitList(null)
            super.submitList(list)
        }
    }

    private inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(contact: Contact, query: Regex) = with (itemView) {
            itemView.text_view_contact_name.text = highlightMatchedWithRegex(contact.displayName, query)
            itemView.text_view_contact_information.text = if (contact.phoneNumbers.size == 1) highlightMatchedWithRegex(contact.phoneNumbers[0].number, query) else context.getString(R.string.t9_search_numbers, contact.phoneNumbers.size)
            itemView.text_view_contact_type.text = if (contact.phoneNumbers.size == 1) contact.phoneNumbers[0].type.name.toLowerCase().capitalize() else ""
            itemView.text_view_contact_icon.setImageBitmap(Contacts().getImageOrPlaceholderForContact(contact))

            setOnClickListener {
                contactWasClicked(contact)
            }
        }

        /**
         * Bolds the part of the string that matches the regex query.
         *
         */
        private fun highlightMatchedWithRegex(str: String, regex: Regex) : Spannable {
            regex.find(str.toLowerCase())?.let {
                val str = SpannableStringBuilder(str)
                str.setSpan(StyleSpan(Typeface.BOLD), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                return str
            }

            return str.toSpannable()
        }

    }

    class DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem?.id == newItem?.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
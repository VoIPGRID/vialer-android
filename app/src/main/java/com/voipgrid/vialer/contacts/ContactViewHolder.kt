package com.voipgrid.vialer.contacts

import android.net.Uri
import android.view.View
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.util.IconHelper
import kotlinx.android.synthetic.main.list_item_contact.view.*
import java.util.*

class ContactViewHolder(itemView: View, private val adapter: ContactAdapter) : RecyclerView.ViewHolder(itemView) {

    fun bind(contact: Contact) = with (itemView) {
        itemView.updatePadding(left = 0)
        itemView.alphabet_character.visibility = if (adapter.isStartOfSection(contact)) View.VISIBLE else View.INVISIBLE
        if (adapter.isStartOfSection(contact)) {
            itemView.alphabet_character.text = adapter.findSectionCharacter(contact).toString().toUpperCase(Locale.getDefault())
        }
        itemView.text_view_contact_name.text = contact.displayName
        itemView.dividerBottom.visibility = if (adapter.isEndOfSection(contact) && adapter.isNotLast(contact)) View.VISIBLE else View.INVISIBLE
        setIcon(contact)

        setOnClickListener {
            ContactsFragment.contactWasClickedCallback?.invoke(contact)
        }
    }

    private fun setIcon(contact: Contact) {
        if (contact.photoUri != null && contact.photoUri.isNotBlank()) {
            itemView.text_view_contact_icon.setImageURI(Uri.parse(contact.photoUri))
            return
        }

        itemView.text_view_contact_icon.setImageBitmap(
                IconHelper.getCallerIconBitmap(contact.displayName.initials(), contact.phoneNumbers[0].toString(), 0)
        )
    }

    /**
     * Extract the initials (first letter from each word) from a string.
     *
     * @return The initials as a new string
     */
    private fun String.initials(limit: Int = 2) : String = this
                .split(" ", limit = limit)
                .filter { it.isNotEmpty() }
                .joinToString(separator = "") {
                    it.first().toUpperCase().toString()
                }
                .filter { char -> char.toLowerCase() in 'a' .. 'z' }
}
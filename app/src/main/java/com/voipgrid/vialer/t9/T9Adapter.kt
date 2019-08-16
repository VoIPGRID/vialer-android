package com.voipgrid.vialer.t9

import android.graphics.Typeface
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.github.tamir7.contacts.Contact
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.text.toSpannable
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.contacts.Contacts
import de.hdodenhof.circleimageview.CircleImageView




class T9Adapter : ArrayAdapter<Contact>(VialerApplication.get(), 0, mutableListOf()) {

    var currentQuery: Regex? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val contact = getItem(position)

        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_contact, parent, false)

        view.findViewById<CircleImageView>(R.id.text_view_contact_icon).setImageBitmap(Contacts().getImageOrPlaceholderForContact(contact))
        view.findViewById<TextView>(R.id.text_view_contact_name).text = contact.displayName
        view.findViewById<TextView>(R.id.text_view_contact_information).text = if (contact.phoneNumbers.size == 1) contact.phoneNumbers[0].number else "${contact.phoneNumbers.size} numbers"
        view.findViewById<TextView>(R.id.text_view_contact_type).text = if (contact.phoneNumbers.size == 1) contact.phoneNumbers[0].type.name.toLowerCase().capitalize() else ""

        currentQuery?.let {
            view.findViewById<TextView>(R.id.text_view_contact_name).text = highlightMatchedWithRegex(contact.displayName, it)
        }

        return view
    }

    private fun highlightMatchedWithRegex(str: String, regex: Regex) : Spannable {
        regex.find(str.toLowerCase())?.let {
            val str = SpannableStringBuilder(str)
            str.setSpan(StyleSpan(Typeface.BOLD), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return str
        }

        return str.toSpannable()
    }
}
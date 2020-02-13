package com.voipgrid.vialer.contacts

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SectionIndexer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.R

class ContactAdapter : ListAdapter<Contact, ContactViewHolder>(DiffCallback()), SectionIndexer {

    private lateinit var sectionPositions: ArrayList<Int>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        return ContactViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.list_item_contact, parent, false),
                this
        )
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<Contact>?) {
        super.submitList(null)
        super.submitList(list)
    }

    override fun getSectionForPosition(position: Int): Int {
        return 0
    }

    override fun getSections(): Array<String> {
        val sections: MutableList<String> = ArrayList()
        sectionPositions = ArrayList()

        currentList.firstOrNull { it.displayName[0].toLowerCase() !in 'a' .. 'z' }?.let {
            sections.add("&")
            sectionPositions.add(currentList.indexOf(it))
        }

        for (char in 'A'..'Z') {
            val contact = currentList.firstOrNull {
                it.displayName.startsWith(char, ignoreCase = true)
            }
            sections.add(char.toString())
            sectionPositions.add(currentList.indexOf(contact))
        }

        currentList.firstOrNull { it.displayName[0].isPotentialPhoneNumber() }?.let {
            sections.add("#")
            sectionPositions.add(currentList.indexOf(it))
        }

        return sections.toTypedArray()
    }

    fun isStartOfSection(contact: Contact): Boolean = sectionPositions.contains(currentList.indexOf(contact))

    fun isEndOfSection(contact: Contact): Boolean = sectionPositions.contains(currentList.indexOf(contact) + 1)

    fun findSectionCharacter(contact: Contact): Char = sections[sectionPositions.indexOf(currentList.indexOf(contact))][0]

    fun isNotLast(contact: Contact): Boolean =
            currentList.last() != contact

    override fun getPositionForSection(sectionIndex: Int): Int {
        return sectionPositions[sectionIndex]
    }
}

class DiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem.id == newItem.id
    }
}

fun Char.isPotentialPhoneNumber(): Boolean = this in '0' .. '9' || this == '+' || this == '('

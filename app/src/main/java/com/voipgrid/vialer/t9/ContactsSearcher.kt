package com.voipgrid.vialer.t9

import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.Contacts.getQuery
import kotlinx.coroutines.*

class ContactsSearcher {

    /**
     * Query the contacts by way of a t9 search.
     *
     */
    suspend fun t9(t9Query: Regex) : List<Contact> = withContext(Dispatchers.Default) {
        contacts.filter {
            it.phoneNumbers.forEach { number ->
                if (number.number.contains(t9Query)) {
                    return@filter true
                }
            }

            it.displayName.toLowerCase().contains(t9Query)
        }
    }

    /**
     * Refresh the contacts that we have fetched to query.
     *
     */
    suspend fun refresh() = withContext(Dispatchers.Default) {
        contacts.clear()
        contacts.addAll(getQuery().hasPhoneNumber().find())
    }

    companion object {
        val contacts = mutableListOf<Contact>()
    }
}
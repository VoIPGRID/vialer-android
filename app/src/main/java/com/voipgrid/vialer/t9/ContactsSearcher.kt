package com.voipgrid.vialer.t9

import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.Contacts.getQuery
import com.github.tamir7.contacts.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsSearcher {

    /**
     * The contacts fields we are loading in the query.
     *
     */
    private val fields = arrayOf(
            Contact.Field.DisplayName,
            Contact.Field.PhoneNumber,
            Contact.Field.PhotoUri,
            Contact.Field.ContactId
    )

    /**
     * Query the contacts by way of a t9 search.
     *
     */
    suspend fun t9(t9Query: Regex) : List<Contact> = withContext(Dispatchers.Default) {
        contacts.asSequence().filter {
            it.displayName.toLowerCase().contains(t9Query) or it.phoneNumbers.any { number -> number.number.contains(t9Query) }
        }.toList()
    }

    /**
     * Refresh the contacts that we have fetched to query.
     *
     */
    suspend fun refresh() = withContext(Dispatchers.Default) {
        contacts.clear()
        contacts.addAll(createQuery().find())
    }

    /**
     * Create the contacts query, we are loading all of these into memory so it should
     * be efficient.
     *
     */
    private fun createQuery() : Query {
        return getQuery()
                .include(*fields)
                .hasPhoneNumber()
    }

    companion object {
        val contacts = mutableListOf<Contact>()
    }
}
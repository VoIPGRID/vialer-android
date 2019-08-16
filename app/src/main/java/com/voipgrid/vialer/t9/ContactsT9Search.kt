package com.voipgrid.vialer.t9

import android.util.Log
import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.Contacts.getQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ContactsT9Search {

    init {
        refresh()
    }

    fun query(query: String, callback: (results: List<Contact>) -> Unit) {

        val regex = convertQueryToRegex(query)

        GlobalScope.launch {
            val contacts = fetchContacts(query, regex)

            GlobalScope.launch(Dispatchers.Main) {
                callback.invoke(contacts)
            }
        }
    }

    private fun fetchContacts(query: String, regex: Regex) : List<Contact> {
        return contacts.filter {
            if (query.isEmpty()) return@filter false

            it.phoneNumbers.forEach { number ->
                if (number.number.contains(regex)) {
                    return@filter true
                }
            }

            it.displayName.toLowerCase().contains(regex)
        }
    }

    /**
     * Refresh the contacts that we have fetched to query.
     *
     */
    fun refresh() {
        GlobalScope.launch {
            contacts = getQuery().hasPhoneNumber().find()
        }
    }

    companion object {
        var contacts = listOf<Contact>()

        private val mappings = mapOf(
                '1' to "[.,!?,;1]{1}",
                '2' to "[abc2]{1}",
                '3' to "[def3]{1}",
                '4' to "[ghi4]{1}",
                '5' to "[jkl5]{1}",
                '6' to "[mno6]{1}",
                '7' to "[pqrs7]{1}",
                '8' to "[tuv8]{1}",
                '9' to "[wxyz9]{1}"
        )

        fun convertQueryToRegex(query: String) : Regex {
            var regexQuery = "^"

            query.forEach {
                regexQuery = regexQuery.plus(mappings[it])
            }

            return Regex(regexQuery)
        }
    }
}
package com.voipgrid.vialer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.t9.ContactsSearcher
import kotlinx.coroutines.launch
import org.koin.core.inject

class ContactsViewModel(private val contactsSearcher: ContactsSearcher) : ViewModel() {

    private val contacts: MutableLiveData<List<Contact>> by lazy {
        MutableLiveData<List<Contact>>().also {
            searchContacts()
        }
    }

    fun getContacts(): LiveData<List<Contact>> {
        return contacts
    }

    fun searchContacts(term: String? = null) {
        viewModelScope.launch {
            contacts.postValue(contactsSearcher.textSearch(term))
        }
    }
}
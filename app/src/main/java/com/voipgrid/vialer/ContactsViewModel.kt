package com.voipgrid.vialer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.permissions.ContactsPermission
import com.voipgrid.vialer.t9.ContactsSearcher
import kotlinx.coroutines.launch

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
        if (!ContactsPermission.hasPermission(VialerApplication.get())) return

        viewModelScope.launch {
            contacts.postValue(contactsSearcher.textSearch(term))
        }
    }
}
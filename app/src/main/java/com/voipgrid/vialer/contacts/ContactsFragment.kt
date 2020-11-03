package com.voipgrid.vialer.contacts

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.tamir7.contacts.Contact
import com.voipgrid.vialer.ContactsViewModel
import com.voipgrid.vialer.R
import com.voipgrid.vialer.contacts.dialog.ContactDialog
import com.voipgrid.vialer.permissions.ContactsPermission
import com.voipgrid.vialer.util.DialHelper
import com.voipgrid.vialer.util.PhoneNumberUtils
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.component.KoinComponent
import kotlin.coroutines.CoroutineContext

class ContactsFragment : Fragment(), KoinComponent, CoroutineScope {

    private lateinit var job: Job
    private var searchView: SearchView? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val adapter = ContactAdapter()

    private val model: ContactsViewModel by sharedViewModel()

    init {
        contactWasClickedCallback = {
            ContactDialog(it.id).show(activity?.supportFragmentManager!!, "contact")
        }

        contactNumberWasClickedCallback = { contact, number ->
            callContact(contact, number)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.fragment_contacts, null).also {
            setHasOptionsMenu(true)
        }

        (activity as AppCompatActivity).apply {
            setSupportActionBar(view.findViewById(R.id.action_bar))
            supportActionBar?.title = getString(R.string.contacts_title)
        }

        if (!ContactsPermission.hasPermission(activity)) {
            findNavController().navigate(R.id.navigation_call_records)
            return view
        }

        model.getContacts().observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        })

        model.searchContacts()

        return view
    }

    override fun onPause() {
        super.onPause()
        searchView?.clearFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.contacts_options_menu, menu)
        searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView?.apply {
            isIconifiedByDefault = true
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    refresh(search = newText)
                    return true
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        job = Job()

        contacts_recycler_view.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            setIndexBarTransparentValue(0.0f)
            setIndexBarStrokeVisibility(false)
            setIndexBarTextColor(R.color.dial_button_digit_color)

            adapter = this@ContactsFragment.adapter
        }
    }

    private fun refresh(search: String? = null) {
        if (!ContactsPermission.hasPermission(activity)) {
            return
        }

        model.searchContacts(search)
    }

    /**
     * Called when a user clicks on a contact in the list.
     *
     */
    private fun callContact(contact: Contact, number: String) {
        startCall(number, contact.displayName)
        return
    }

    /**
     * Start a call to this number and contact.
     *
     */
    private fun startCall(number: String, contact: String) {
        activity?.let {
            DialHelper.fromActivity(it).callNumber(PhoneNumberUtils.format(number), contact)
        }
    }

    companion object {
        var contactWasClickedCallback: ((contact: Contact) -> Unit)? = null
        var contactNumberWasClickedCallback: ((contact: Contact, number: String) -> Unit)? = null
    }
}
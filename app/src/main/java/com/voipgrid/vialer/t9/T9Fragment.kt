package com.voipgrid.vialer.t9

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import butterknife.Unbinder
import com.github.tamir7.contacts.PhoneNumber
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.fragment_t9_search.*

class T9Fragment : Fragment(), AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

    private val helper = T9HelperFragment()
    private val t9 = ContactsT9Search()
    private val adapter = T9Adapter()
    var listener: Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_t9_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.beginTransaction().add(R.id.helper, helper).commit()
        t9.refresh()

        list_view.adapter = adapter
        list_view.onItemClickListener = this
        list_view.setOnScrollListener(this)
    }

    fun search(query: String) {
        if (query.isEmpty()) {
            helper.show()
            list_view.visibility = View.GONE
            return
        }

        adapter.currentQuery = ContactsT9Search.convertQueryToRegex(query)

        t9.query(query) {
            helper.hide()
            list_view.visibility = View.VISIBLE
            adapter.clear()
            adapter.addAll(it)
            adapter.notifyDataSetChanged()
        }
    }

    fun show() {
        view?.visibility = View.VISIBLE
    }

    fun hide() {
        view?.visibility = View.GONE
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val contact = adapter.getItem(position) ?: return

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

    override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
            listener?.onExpandRequested()
        }
    }

    interface Listener {
        fun onExpandRequested()

        fun onContactSelected(number: String, name: String)
    }
}
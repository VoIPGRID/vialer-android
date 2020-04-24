package com.voipgrid.vialer

import android.database.Cursor
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import com.github.tamir7.contacts.Contact

/**
 * Listens for a click on the drawable on the right hand side of the edit text
 * and executes the callback.
 *
 */
fun EditText.setRightDrawableOnClickListener(callback: () -> Unit) {
    setOnTouchListener { _: View, event: MotionEvent -> Boolean
        val right = 2

        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= (this.right - compoundDrawables[right].bounds.width())) {
                callback()
                return@setOnTouchListener true
            }
        }

        return@setOnTouchListener false
    }
}

/**
 * Set an onClick listener but will automatically disable the button after it has been clicked once.
 *
 */
fun Button.setOnClickListenerAndDisable(function: (View) -> Unit) {
    setOnClickListener {
        it.isEnabled = false
        function.invoke(it)
    }
}

/**
 * Set an onClick listener but will automatically disable the button after it has been clicked once.
 *
 */
fun Button.setOnClickListenerAndDisable(listener: View.OnClickListener) {
    setOnClickListener {
        it.isEnabled = false
        listener.onClick(it)
    }
}

/**
 * Fill a specified field on the WebView, these settings MUST be true: domStorageEnabled, javaScriptEnabled
 *
 */
fun WebView.fillField(fieldId: String, value: String) {
    val js = "javascript:var uselessvar = document.getElementById('$fieldId').value = '$value';"
    evaluateJavascript(js) {}
}

fun EditText.onTextChanged(callback: (Editable?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            callback(p0)
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
    })
}

fun Contact.findLookupId() : String {
    val cContactIdString = ContactsContract.Contacts._ID
    val cCONTACT_CONTENT_URI = ContactsContract.Contacts.CONTENT_URI
    val cDisplayNameColumn = ContactsContract.Contacts.LOOKUP_KEY
    val selection = "$cContactIdString = ? "
    val selectionArgs = arrayOf(id.toString())
    val cursor: Cursor = VialerApplication.get()?.contentResolver?.query(cCONTACT_CONTENT_URI, null, selection, selectionArgs, null)!!
    if (cursor.count > 0) {
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            if (cursor.getColumnIndex(cContactIdString) >= 0) {
                if (id == cursor.getLong(cursor.getColumnIndex(cContactIdString))) {
                    return cursor.getString(cursor.getColumnIndex(cDisplayNameColumn))
                }
            }
            cursor.moveToNext()
        }
    }
    cursor.close()
    return ""
}
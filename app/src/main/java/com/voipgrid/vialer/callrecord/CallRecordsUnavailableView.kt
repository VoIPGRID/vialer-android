package com.voipgrid.vialer.callrecord

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.call_records_unavailable_view.view.*

class CallRecordsUnavailableView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.call_records_unavailable_view, this)
    }

    /**
     * Update to show a permissions failed message.
     *
     */
    fun permissionsFailed() {
        render(R.string.call_records_unauthorized_title, R.string.call_records_unauthorized_message)
    }

    /**
     * Update to show a generic unavailable message.
     *
     */
    fun unavailable() {
        render(R.string.call_records_default_title, R.string.call_records_default_message)
    }

    /**
     * Update to show a no internet connection message.
     *
     */
    fun noInternetConnection() {
        render(R.string.call_records_no_internet_title, R.string.call_records_no_internet_message)
    }

    /**
     * Update to show a no missed calls exist message.
     *
     */
    fun noMissedCalls() {
        render(R.string.call_records_no_missed_title, R.string.call_records_no_missed_message)
    }

    /**
     * Hide the view completely.
     *
     */
    fun hide() {
        visibility = View.GONE
    }

    /**
     * Render a message to the view and make it visible.
     *
     */
    private fun render(title: Int, content: Int) {
        messageTitle.text = context.getText(title).toString().capitalize()
        messageContent.setText(content)
        visibility = View.VISIBLE
    }
}
package com.voipgrid.voip.ui

import android.text.format.DateUtils
import com.voipgrid.contacts.Contacts
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.text.DateFormat

class Display(private val number: String, private val name: String, private val duration: Int): KoinComponent {

    private val contacts: Contacts by inject()

    private val contactName
        get() = contacts.getContactNameByPhoneNumber(number)

    private val displayName
        get() = if (contactName?.isNotBlank() == true) contactName else name

    private val hasAName
        get() = displayName?.isNotBlank() == true

    /**
     * This will guess that the "header" should be populated by the display name and then the number
     * based on what is available.
     */
    val heading
        get() = if (hasAName) displayName else number

    /**
     * This will guess that the subheading will be populated by the number if there is a name available
     * otherwise it will be blank.
     *
     */
    val subheading
        get() = if (hasAName) number else ""

    val prettyDuration
        get() = DateUtils.formatElapsedTime(duration.toLong())
}
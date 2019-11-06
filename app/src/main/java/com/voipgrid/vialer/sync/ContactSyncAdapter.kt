package com.voipgrid.vialer.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.*
import android.content.Context.ACCOUNT_SERVICE
import android.os.Bundle
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import javax.inject.Inject

class ContactSyncAdapter : AbstractThreadedSyncAdapter {

    private val profileMimeType: String
    private val contentResolver: ContentResolver

    @Inject lateinit var dao: CallRecordDao

    constructor(context: Context, autoInitialize: Boolean) : super(context, autoInitialize) {
        contentResolver = context.contentResolver
        profileMimeType = context.getString(R.string.call_mime_type)
        VialerApplication.get().component().inject(this)
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    constructor(context: Context, autoInitialize: Boolean, allowParallelSyncs: Boolean) : super(context, autoInitialize, allowParallelSyncs) {
        contentResolver = context.contentResolver
        profileMimeType = context.getString(R.string.call_mime_type)
        VialerApplication.get().component().inject(this)
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        if (account.type != context.getString(R.string.account_type)) {
            return
        }
        val contactManager = ContactManager(account, contentResolver)
        val nativeContacts = contactManager.rawContacts
        if (nativeContacts.isEmpty()) {
            return
        }

        val vialerContacts = contactManager.vialerContacts
        val uniqueNumbers = dao.listThirdPartyNumber()
        val processedNumbers = ArrayList<String>()
        //sync the native contacts
        for (contact in nativeContacts) {
            //a native contact can have more than one phone number, sync all phone numbers
            for(number in contact.phoneNumbers) {
                //skip number if it's already processed or if you never called it with Vialer before
                if (number == null || number.isEmpty() || processedNumbers.contains(number) || !uniqueNumbers.contains(number)) {
                    continue
                }
                processedNumbers.add(number)
                //find the vialer contact that's linked to the this native contact
                val existingVialerContact: RawContact? = vialerContacts.find {
                    it.contactId == contact.contactId
                }
                if (existingVialerContact == null) {
                    //if there's no vialer contact, create one and merge it with the native contact
                    val rawContactId = contactManager.addContact(context, contact.firstName
                            ?: "", contact.lastName ?: "", contact.displayName ?: "", number)
                    contactManager.mergeContact(contact.rawContactId, rawContactId)
                } else if (!contactManager.hasVialerCallSupport(context, number)) {
                    //if the vialer contact has no 'vialer call support' for the {@code number}
                    //then insert it
                    contactManager.insertVialerCallSupport(existingVialerContact.rawContactId, number, context)
                }
            }
        }
        //it could be that a native contact got deleted, or that a number got deleted,
        //however the Vialer contact and its data will still exist
        //loop through all vialer contacts and check if the native contact still exists
        for (vialerContact in vialerContacts) {
            //this 'find' may take a bit long (~3 sec) if you have a lot of contacts (like ~250)
            val existingNativeContact: RawContact? = nativeContacts.find {
                it.contactId == vialerContact.contactId
            }
            val vialerNumbers = contactManager.getVialerCallSupportNumbers(vialerContact.rawContactId, context)
            //a user may delete a number at anytime and at any moment
            //delete the vialer call support for the numbers that don't exist anymore
            for(number in vialerNumbers) {
                if (vialerContact.phoneNumbers.contains(number)) {
                    continue
                }
                contactManager.deleteVialerCallSupport(vialerContact.rawContactId, number, context)
            }
            //check if the vialer contact has no phone numbers and the native contacts has no phone numbers
            //in that case, delete the vialer contact
            if (vialerContact.phoneNumbers.isNotEmpty() && existingNativeContact?.phoneNumbers?.isNotEmpty() ?: continue) {
                continue
            }
            contactManager.deleteVialerContact(context, vialerContact.rawContactId, vialerContact.contactId
                    ?: continue)
        }
    }

    companion object {
        fun createSyncAccount(context: Context): Account? {
            val newAccount = Account(context.getString(R.string.account_name), context.getString(R.string.account_type))
            val accountManager = context.getSystemService(ACCOUNT_SERVICE) as AccountManager
            val success = accountManager.addAccountExplicitly(newAccount, null, null)
            if (success) {
                return newAccount
            }
            val accounts = accountManager.getAccountsByType(context.getString(R.string.account_type))
            for (account in accounts) {
                if (account.name == newAccount.name) {
                    return account
                }
            }
            return null
        }
    }

}

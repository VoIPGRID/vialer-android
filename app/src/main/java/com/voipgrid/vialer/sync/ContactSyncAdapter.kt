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
import android.content.ContentResolver

class ContactSyncAdapter : AbstractThreadedSyncAdapter {

    private val profileMimeType: String
    private val contentResolver: ContentResolver

    @Inject
    lateinit var dao: CallRecordDao

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
        if (account.name != context.getString(R.string.account_name)) {
            return
        }
        val contactManager = ContactManager(account, contentResolver)
        val localContacts = contactManager.rawContacts
        if (localContacts.isEmpty()) {
            return
        }
        val vialerContacts = contactManager.vialerContacts
        val uniqueNumbers = dao.listThirdPartyNumber()
        val list = ArrayList<String>()
        //the real syncing part
        for (contact in localContacts) {
            val number = contact.mobileNumber
            if (number == null || number.isEmpty() || list.contains(number) || !uniqueNumbers.contains(number)) {
                continue
            }
            val existingVialerContact: RawContact? = vialerContacts.find {
                it.contactId == contact.contactId
            }
            if (existingVialerContact == null) {
                val rawContactId = contactManager.addContact(context, contact.firstName
                        ?: "", contact.lastName ?: "", contact.displayName ?: "", number)
                contactManager.mergeContact(contact.rawContactId, rawContactId)
            }
            list.add(number)
        }
        for (vialerContact in vialerContacts) {
            val existingLocalContact: RawContact? = localContacts.find {
                it.contactId == vialerContact.contactId
            }
            if (existingLocalContact != null) {
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

package com.voipgrid.vialer.contacts

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.ContactsContract
import androidx.annotation.RequiresApi
import androidx.work.*
import java.util.concurrent.TimeUnit

class ImportContactsForT9Search(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val updatedContacts = getUpdatedContactsCursor()

        if (updatedContacts != null && updatedContacts.count > 0) {
            ContactsSyncTask(context).sync(updatedContacts)
        }
        else {
            updatedContacts?.close()
        }

        schedule()

        return Result.success()
    }


    /**
     * Get the selection string for contact query.
     * @return
     */
    private fun getSelection(): String {
        // Contacts who have a phone number and are changed since last sync.
        return ContactsContract.Data.HAS_PHONE_NUMBER + " = 1 AND " +
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " > " +
                SyncUtils.getLastSync(context)
    }

    /**
     * Get a cursor with the contacts that are updated.
     * @return
     */
    private fun getUpdatedContactsCursor(): Cursor? {
        return context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                getSelection(), null, null, null)
    }

    companion object {

        /**
         * Schedule contact import, using different methods based on the api.
         *
         */
        fun schedule() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                scheduleWhenContentUriIsSupported()
            } else {
                schedulePeriodic()
            }
        }

        /**
         * Automatically import the contacts.
         *
         */
        fun run() {
            WorkManager.getInstance().enqueueUniqueWork(
                    ImportContactsForT9Search::class.java.name,
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<ImportContactsForT9Search>().build()
            )
        }

        /**
         * Schedule a task that will periodically update the contacts, purely based on time.
         *
         */
        private fun schedulePeriodic() {
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                    ImportContactsForT9Search::javaClass.name,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<ImportContactsForT9Search>(1, TimeUnit.HOURS).build()
            )
        }

        /**
         * Schedule to run the task whenever the contacts are updated.
         *
         */
        @RequiresApi(Build.VERSION_CODES.N)
        private fun scheduleWhenContentUriIsSupported() {
            val builder = Constraints.Builder()
            builder.addContentUriTrigger(ContactsContract.Contacts.CONTENT_URI, true)
            builder.setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
            WorkManager.getInstance().enqueueUniqueWork(
                    ImportContactsForT9Search::javaClass.name,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<ImportContactsForT9Search>()
                            .setConstraints(builder.build())
                            .build()
            )
        }
    }
}
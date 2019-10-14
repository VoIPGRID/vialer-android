package com.voipgrid.vialer.api

import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.models.PhoneAccount
import org.joda.time.DateTime
import retrofit2.Call
import retrofit2.Response

/**
 * This class exists to provide a caching layer between when fetching PhoneAccounts, this is because
 * it is a task that we need to perform on potentially hundreds of call records and the actual data
 * being retrieved rarely changes, so we do not want to perform the same request very often.
 *
 */
class PhoneAccountFetcher(private val api : VoipgridApi) {

    private var cache: PhoneAccountsCache = PhoneAccountsCache()

    /**
     * Fetch a phone account, this has a caching layer in-front of it so
     * it can be called frequently without spamming network requests
     * for the same record.
     *
     */
    fun fetch(id: String, callback: Callback) {
        cache = loadCacheFromSharedPreferences()

        if (cache.isOutdated()) {
            invalidateCache()
        }

        val account = cache[id]

        if (account != null) {
            callback.onSuccess(account)
            return
        }

        api.phoneAccount(id).enqueue(HttpHandler(id, callback))
    }

    /**
     * Completed resets our cache from both in-memory and shared
     * preferences.
     *
     */
    private fun invalidateCache() {
        cache = PhoneAccountsCache()
        User.internal.phoneAccounts = null
        User.internal.phoneAccountsCache = null
    }

    /**
     * Attempt to check shared preferences for a version of our PhoneAccounts cache.
     *
     */
    private fun loadCacheFromSharedPreferences() : PhoneAccountsCache {
        if (!cache.isEmpty()) return cache

        val storedCache : PhoneAccountsCache? = User.internal.phoneAccountsCache

        if (storedCache != null) {
            return storedCache
        }

        return PhoneAccountsCache()
    }

    /**
     * The HTTP handler receives responses from the retrofit.
     *
     */
    inner class HttpHandler(private val id: String, private val callback: Callback) : retrofit2.Callback<PhoneAccount> {
        override fun onFailure(call: Call<PhoneAccount>, t: Throwable) {

        }

        override fun onResponse(call: Call<PhoneAccount>, response: Response<PhoneAccount>) {
           if (!response.isSuccessful) {
               return
           }

            val account = response.body() ?: return

            cache[id] = account

            callback.onSuccess(account)

            User.internal.phoneAccountsCache = cache
        }

    }

    /**
     * Provide a callback for when the fetcher has found a phone account.
     *
     */
    interface Callback {
        fun onSuccess(phoneAccount: PhoneAccount)
    }

    /**
     * Provides a class to cache the phone accounts that are found.
     *
     */
    class PhoneAccountsCache : HashMap<String, PhoneAccount>() {

        private val initialTime = DateTime()

        /**
         * Check if the data held by this cache is outdated, if so this should
         * be invalidated and the information reset.
         *
         */
        fun isOutdated() : Boolean {
            return initialTime.isBefore(DateTime().minusDays(DAYS_VALID_FOR))
        }

        companion object {

            /**
             * We will store the records for this many days before invalidating the cache
             * completely and re-fetching the records.
             *
             */
            const val DAYS_VALID_FOR = 3
        }
    }
}
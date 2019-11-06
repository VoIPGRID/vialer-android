package com.voipgrid.vialer.sync

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.NetworkErrorException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder

class ContactAuthenticatorService : Service() {

    private var mAuthenticator: ContactAuthenticator? = null
    override fun onCreate() {
        mAuthenticator = ContactAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mAuthenticator?.iBinder
    }

    class ContactAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

        override fun editProperties(
                r: AccountAuthenticatorResponse, s: String): Bundle {
            throw UnsupportedOperationException()
        }

        @Throws(NetworkErrorException::class)
        override fun addAccount(
                r: AccountAuthenticatorResponse,
                s: String,
                s2: String,
                strings: Array<String>,
                bundle: Bundle): Bundle? {
            return null
        }

        @Throws(NetworkErrorException::class)
        override fun confirmCredentials(
                r: AccountAuthenticatorResponse,
                account: Account,
                bundle: Bundle): Bundle? {
            return null
        }

        @Throws(NetworkErrorException::class)
        override fun getAuthToken(
                r: AccountAuthenticatorResponse,
                account: Account,
                s: String,
                bundle: Bundle): Bundle {
            throw UnsupportedOperationException()
        }

        override fun getAuthTokenLabel(s: String): String {
            throw UnsupportedOperationException()
        }

        @Throws(NetworkErrorException::class)
        override fun updateCredentials(
                r: AccountAuthenticatorResponse,
                account: Account,
                s: String, bundle: Bundle): Bundle {
            throw UnsupportedOperationException()
        }

        @Throws(NetworkErrorException::class)
        override fun hasFeatures(
                r: AccountAuthenticatorResponse,
                account: Account, strings: Array<String>): Bundle {
            throw UnsupportedOperationException()
        }

    }

}
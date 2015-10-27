package com.voipgrid.vialer.contacts;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.voipgrid.vialer.R;


/**
 * The Authenticator activity.
 *
 * Called by the Authenticator and in charge of identifing the user.
 *
 * It sends back to the Authenticator the result.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();
    private AccountManager mAccountManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent res = new Intent();
        res.putExtra(AccountManager.KEY_ACCOUNT_NAME, getString(R.string.contacts_app_name));
        res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        res.putExtra(AccountManager.KEY_AUTHTOKEN, getString(R.string.account_token));

        Account account = new Account(getString(R.string.contacts_app_name), getString(R.string.account_type));
        mAccountManager = AccountManager.get(this);
        mAccountManager.addAccountExplicitly(account, null, null);

        ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
        setAccountAuthenticatorResult(res.getExtras());
        setResult(RESULT_OK, res);
        finish();
    }
}

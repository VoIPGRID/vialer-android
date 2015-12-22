package com.voipgrid.vialer.sip;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.OnRegStateParam;

/**
 * Created by eltjo on 02/09/15.
 */
public interface AccountStatus {
    void onAccountRegistered(Account account, OnRegStateParam param);
    void onAccountUnregistered(Account account, OnRegStateParam param);
    void onAccountInvalidState(Account account, Throwable fault);
}

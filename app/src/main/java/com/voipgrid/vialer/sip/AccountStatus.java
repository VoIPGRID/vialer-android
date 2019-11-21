package com.voipgrid.vialer.sip;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.OnRegStateParam;

public interface AccountStatus {
    void onAccountRegistered(Account account, OnRegStateParam param);
}

package com.voipgrid.vialer.sip;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;


/**
 * Class that reflects a sip account and handles registration.
 */
class SipAccount extends org.pjsip.pjsua2.Account {
    // Callback handler for the onIncomingCall and onRegState events.
    private final AccountStatus mAccountStatus;
    private SipService mSipService;

    /**
     *
     * @param accountConfig configuration to automagically communicate and setup some sort of
     *                      SIP session.
     * @param accountStatus callback object which is used to notify outside world of past events.
     * @throws Exception issue with creating an account.
     */
    public SipAccount(SipService sipService, AccountConfig accountConfig, AccountStatus accountStatus) throws Exception {
        super();
        mAccountStatus = accountStatus;
        mSipService = sipService;
        // Calling create also registers at the server.
        create(accountConfig);
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @param incomingCallParam parameters containing the state of an incoming call.
     */
    @Override
    public void onIncomingCall(OnIncomingCallParam incomingCallParam) {
        SipCall sipCall = new SipCall(mSipService, this, incomingCallParam.getCallId());
        sipCall.onCallIncoming();

        if (mSipService != null && mSipService.getIncomingCallDetails() != null) {
            sipCall.setMiddlewareKey(mSipService.getIncomingCallDetails().getStringExtra(SipConstants.EXTRA_REQUEST_TOKEN));
        }
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @param regStateParam parameters containing the state of this registration.
     */
    @Override
    public void onRegState(OnRegStateParam regStateParam) {
        try {
            AccountInfo info = getInfo();
            if (info.getRegIsActive()) {
                mAccountStatus.onAccountRegistered(this, regStateParam);
            } else {
                mAccountStatus.onAccountUnregistered(this, regStateParam);
            }
        } catch (Exception exception) {
            mAccountStatus.onAccountInvalidState(this, exception);
        }
    }
}

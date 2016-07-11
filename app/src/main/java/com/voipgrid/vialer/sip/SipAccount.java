package com.voipgrid.vialer.sip;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;

/* Regulates SipCall event management and connection to server. Delegates to SIPInterface. */
class SipAccount extends org.pjsip.pjsua2.Account {
    // Callback handler for the onIncomingCall and onRegState events.
    private final AccountStatus mAccountStatus;
    private final CallStatus mCallStatus;

    /**
     *
     * @param accountConfig configuration to automagically communicate and setup some sort of
     *                      SIP session.
     * @param accountStatus callback object which is used to notify outside world of past events.
     * @param callStatus callback object which is used to notify outside world of past events.
     * @throws Exception issue with creating an account.
     */
    public SipAccount(AccountConfig accountConfig, AccountStatus accountStatus,
                      CallStatus callStatus) throws Exception {
        super();
        mAccountStatus = accountStatus;
        mCallStatus = callStatus;
        /* This automatically registers to the server */
        create(accountConfig);
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     * @see CallStatus
     *
     * @param incomingCallParam parameters containing the state of an incoming call.
     */
    @Override
    public void onIncomingCall(OnIncomingCallParam incomingCallParam) {
        SipCall sipCall = new SipCall(this, incomingCallParam.getCallId(), mCallStatus);
        mCallStatus.onCallIncoming(sipCall);
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @see CallStatus
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

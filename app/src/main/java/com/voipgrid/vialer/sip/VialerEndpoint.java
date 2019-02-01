package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.logging.Logger;

import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.OnTransportStateParam;
import org.pjsip.pjsua2.pjsip_transport_state;

public class VialerEndpoint extends Endpoint {

    private final Logger mLogger;
    private SipService mSipService;

    public VialerEndpoint() {
        super();
        mLogger = new Logger(this.getClass());
    }

    @Override
    public void onTransportState(OnTransportStateParam prm) {
        super.onTransportState(prm);

        if (!prm.getState().equals(pjsip_transport_state.PJSIP_TP_STATE_CONNECTED)) {
            return;
        }

        if (mSipService == null || mSipService.getCurrentCall() == null) {
            return;
        }

        SipCall sipCall = mSipService.getCurrentCall();

        if (sipCall.isIpChangeInProgress()) {
            return;
        }

        try {
            sipCall.reinvite(new CallOpParam(true));
            mLogger.i("There has been a new transport created. Reinivite the calls to keep the call going.");
        } catch (Exception e) {
            mLogger.e("Unable to reinvite call");
        }
    }

    public void setSipService(SipService sipService) {
        mSipService = sipService;
    }
}

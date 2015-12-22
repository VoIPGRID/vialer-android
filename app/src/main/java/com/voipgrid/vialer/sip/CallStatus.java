package com.voipgrid.vialer.sip;

import android.net.Uri;

import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;

/**
 * Manage the callbacks coming from PSJUA2.
 */
public interface CallStatus {
    void onCallIncoming(Call call);
    void onCallOutgoing(Call call, Uri phoneNumber);
    void onCallConnected(Call call);
    void onCallDisconnected(Call call);
    void onCallInvalidState(Call call, Throwable fault);
    void onCallMediaAvailable(Call call, AudioMedia media);
    void onCallMediaUnavailable(Call call);
    void onCallStartRingback();
    void onCallStopRingback();
}

package com.voipgrid.vialer.sip;

import org.pjsip.pjsua2.Call;

/**
 * Created by karsten on 02/07/15.
 */
public interface CallInteraction {
    void hangUp(Call call);
    void answer(Call call);
    void decline(Call call);
    void updateMicrophoneVolume(Call call, long newVolume);
    void putOnHold(Call call);
    void xFer(org.pjsip.pjsua2.Call call);
}

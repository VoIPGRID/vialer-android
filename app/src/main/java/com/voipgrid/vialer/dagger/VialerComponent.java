package com.voipgrid.vialer.dagger;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.IncomingCallActivity;
import com.voipgrid.vialer.calling.PendingCallActivity;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.sip.SipService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {VialerModule.class})
public interface VialerComponent {
    void inject(VialerApplication app);

    void inject(AbstractCallActivity abstractCallActivity);

    void inject(CallActivity callActivity);

    void inject(IncomingCallActivity incomingCallActivity);

    void inject(SipService sipService);

    void inject(PendingCallActivity pendingCallActivity);

    void inject(DialerActivity dialerActivity);

    void inject(CallRecordAdapter callRecordAdapter);
}

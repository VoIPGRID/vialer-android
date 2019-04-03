package com.voipgrid.vialer.dagger;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.InternalNumbers;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.IncomingCallActivity;
import com.voipgrid.vialer.calling.NetworkAvailabilityActivity;
import com.voipgrid.vialer.calling.PendingCallActivity;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.callrecord.CallRecordFragment;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.notifications.call.AbstractCallNotification;
import com.voipgrid.vialer.sip.NetworkConnectivity;
import com.voipgrid.vialer.sip.SipService;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

import androidx.annotation.Nullable;
import dagger.Component;

@Singleton
@Component(modules = {VialerModule.class})
public interface VialerComponent {
    void inject(VialerApplication app);

    void inject(AbstractCallActivity abstractCallActivity);

    void inject(CallActivity callActivity);

    void inject(IncomingCallActivity incomingCallActivity);

    void inject(NetworkAvailabilityActivity networkAvailabilityActivity);

    void inject(SipService sipService);

    void inject(PendingCallActivity pendingCallActivity);

    void inject(DialerActivity dialerActivity);

    void inject(CallRecordAdapter callRecordAdapter);

    void inject(NetworkConnectivity networkConnectivity);

    Preferences getPreferences();

    void inject(CallRecordFragment fragment);

    void inject(CallRecord callRecord);

    @Nullable
    InternalNumbers getInternalNumbers();

    void inject(@NotNull AbstractCallNotification callNotification);
}

package com.voipgrid.vialer.dagger;

import com.voipgrid.vialer.ForgottenPasswordActivity;
import com.voipgrid.vialer.Logout;
import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.callrecord.CallRecordAdapter;
import com.voipgrid.vialer.callrecord.CallRecordFragment;
import com.voipgrid.vialer.callrecord.CallRecordViewHolder;
import com.voipgrid.vialer.callrecord.CallRecordViewModel;
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.dialer.NumberInputView;
import com.voipgrid.vialer.logging.VialerBaseActivity;
import com.voipgrid.vialer.notifications.call.AbstractCallNotification;
import com.voipgrid.vialer.onboarding.OnboardingActivity;
import com.voipgrid.vialer.onboarding.steps.AccountConfigurationStep;
import com.voipgrid.vialer.onboarding.steps.LoginStep;
import com.voipgrid.vialer.onboarding.steps.MissingVoipAccountStep;
import com.voipgrid.vialer.settings.AccountSettingsFragment;
import com.voipgrid.vialer.settings.SettingsFragment;
import com.voipgrid.vialer.t9.T9Fragment;
import com.voipgrid.vialer.t9.T9HelperFragment;
import com.voipgrid.vialer.util.LoginRequiredActivity;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {VialerModule.class})
public interface VialerComponent {
    void inject(VialerApplication app);

    void inject(CallRecordAdapter callRecordAdapter);

    void inject(CallRecord callRecord);

    SystemUser getSystemUser();

    void inject(CallRecordViewHolder callRecordViewHolder);

    void inject(LoginRequiredActivity loginRequiredActivity);

    void inject(MainActivity activity);

    void inject(VialerBaseActivity vialerBaseActivity);

    void inject(T9HelperFragment t9HelperFragment);

    void inject(T9Fragment t9Fragment);

    void inject(AbstractCallNotification notification);

    void inject(NumberInputView numberInputView);

    void inject(ForgottenPasswordActivity forgottenPasswordActivity);

    void inject(LoginStep loginStep);

    void inject(AccountConfigurationStep accountConfigurationStep);

    void inject(OnboardingActivity onboardingActivity);

    void inject(MissingVoipAccountStep missingVoipAccountStep);

    void inject(CallRecordFragment callRecordFragment);

    void inject(HistoricCallRecordsImporter.Worker worker);

    void inject(DialerActivity dialerActivity);

    Logout provideLogout();

    void inject(@NotNull final CallRecordViewModel callRecordViewModel);
}

package com.voipgrid.vialer.dagger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class VialerModule {
    private final VialerApplication mVialerApplication;

    public VialerModule(VialerApplication vialerApplication) {
        mVialerApplication = vialerApplication;
    }

    @Singleton
    @Provides
    Context provideContext() {
        return mVialerApplication;
    }

    @Singleton
    @Provides
    TelephonyManager provideTelephonyManager() {
        return (TelephonyManager) mVialerApplication.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Singleton
    @Provides
    JsonStorage provideJsonStorage() {
        return new JsonStorage(mVialerApplication);
    }

    @Provides
    ConnectivityManager provideCconnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    ConnectivityHelper provideConnectivityHelper(TelephonyManager telephonyManager, ConnectivityManager connectivityManager) {
        return new ConnectivityHelper(connectivityManager, telephonyManager);
    }

    @Provides
    SystemUser provideSystemUser(JsonStorage jsonStorage) {
        return (SystemUser) jsonStorage.get(SystemUser.class);
    }

    @Provides
    PhoneAccount providePhoneAccount(JsonStorage jsonStorage) {
        return (PhoneAccount) jsonStorage.get(PhoneAccount.class);
    }
}

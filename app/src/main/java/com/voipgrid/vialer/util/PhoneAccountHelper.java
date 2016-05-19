package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerGcmRegistrationService;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;


/**
 * AsyncTask for updating a users phone account.
 */
public class PhoneAccountHelper {

    private Api mApi;
    private Context mContext;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;

    public PhoneAccountHelper(Context context) {
        mContext = context;

        mPreferences = new Preferences(context);
        mJsonStorage = new JsonStorage(context);

        // Get credentials for api.
        SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        String username = systemUser.getEmail();
        String password = systemUser.getPassword();

        mApi = ServiceGenerator.createPortalService(
                mContext,
                Api.class,
                username,
                password
        );
    }

    /**
     * Function to get the current systemuser from the api.
     * @return
     */
    public SystemUser getAndUpdateSystemUser() {
        Call<SystemUser> call = mApi.systemUser();
        SystemUser systemUser = null;
        try {
            Response<SystemUser> response = call.execute();
            if (response.isSuccess() && response.body() != null) {
                systemUser = response.body();
                updateSystemUser(systemUser);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return systemUser;
    }

    /**
     * Function to update the systemuser information.
     */
    private void updateSystemUser(SystemUser systemUser) {
        SystemUser currentSystemuser = (SystemUser) mJsonStorage.get(SystemUser.class);
        currentSystemuser.setOutgoingCli(systemUser.getOutgoingCli());
        currentSystemuser.setMobileNumber(systemUser.getMobileNumber());
        mJsonStorage.save(currentSystemuser);
    }


    /**
     * Function to get the phone linked to the user. This also updates the systemuser.
     * @return PhoneAccount object or null.
     */
    public PhoneAccount getLinkedPhoneAccount() {
        SystemUser systemUser = getAndUpdateSystemUser();
        PhoneAccount phoneAccount = null;

        if (systemUser != null) {
            mPreferences.setSipPermission(systemUser.hasSipPermission());
            String phoneAccountId = systemUser.getPhoneAccountId();

            // Get phone account from API if one is provided in the systemuser API.
            if (phoneAccountId != null) {

                // If no PhoneAccountId is returned, remove current PhoneAccount information from jsonstorage.
                new JsonStorage(mContext).remove(PhoneAccount.class);

                Call<PhoneAccount> phoneAccountCall = mApi.phoneAccount(phoneAccountId);
                try {
                    Response<PhoneAccount> phoneAccountResponse = phoneAccountCall.execute();
                    if (phoneAccountResponse.isSuccess() && phoneAccountResponse.body() != null) {
                        phoneAccount = phoneAccountResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return phoneAccount;
    }

    public void savePhoneAccountAndRegister(PhoneAccount phoneAccount) {
        // Check if we have something to save and register.
        if (phoneAccount == null) {
            return;
        }

        // Get the phone account currently saved in the local storage.
        PhoneAccount existingPhoneAccount = ((PhoneAccount) mJsonStorage.get(PhoneAccount.class));
        // Save the linked phone account in the local storage.
        mJsonStorage.save(phoneAccount);

        // Check if the user can use sip and if the phone account changed or unregistered.
        if (mPreferences.canUseSip()) {
            boolean register = false;
            if (!phoneAccount.equals(existingPhoneAccount)) {
                // New registration because phone account changed.
                MiddlewareHelper.setRegistrationStatus(mContext,
                        MiddlewareHelper.Constants.STATUS_UPDATE_NEEDED);
                register = true;
            } else if (MiddlewareHelper.needsRegistration(mContext)) {
                // New registration because we need a registration.
                register = true;
            }

            if (register) {
                startMiddlewareRegistrationService();
            }
        }
    }

    /**
     * Function for start the service that handles the middleware registration.
     */
    private void startMiddlewareRegistrationService() {
        mContext.startService(new Intent(mContext, VialerGcmRegistrationService.class));
    }

    /**
     * Function to update the phone account and register at the middleware if it changed.
     */
    public void updatePhoneAccount() {
        // Get the phone account that is linked to user.
        PhoneAccount linkedPhoneAccount = getLinkedPhoneAccount();

        if (linkedPhoneAccount != null) {
            savePhoneAccountAndRegister(linkedPhoneAccount);
        } else {
            // User has no phone account linked so remove it from the local storage.
            mJsonStorage.remove(PhoneAccount.class);
        }
    }

    public void executeUpdatePhoneAccountTask() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                updatePhoneAccount();
                return null;
            }
        }.execute();
    }
}

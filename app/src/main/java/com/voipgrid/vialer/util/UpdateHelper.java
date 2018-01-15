package com.voipgrid.vialer.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.OnUpdateCompleted;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.PreviousRequestNotFinishedException;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UseEncryption;
import com.voipgrid.vialer.logging.RemoteLogger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Class to setup the app to work with the newest code.
 * All methods in this class should only be run once.
 */
public class UpdateHelper extends AsyncTask<Void, Void, Void> implements AppVersions, Callback<UseEncryption> {

    private RemoteLogger mRemoteLogger;
    private String mVersionName;
    private Context mContext;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;
    private ProgressDialog mProgressDialog;
    private OnUpdateCompleted mListener;
    private final static String VERSION_CODE = "version_code";
    private Boolean succesfulMigrate = true;

    public UpdateHelper(Context context, OnUpdateCompleted listener) {
        mContext = context;
        mJsonStorage = new JsonStorage(mContext);
        mPreferences = new Preferences(mContext);
        this.mListener = listener;

        mRemoteLogger = new RemoteLogger(UpdateHelper.class);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = ProgressDialog.show(
                mContext, mContext.getString(R.string.update_spinner_title),
                mContext.getString(R.string.update_spinner_message), true);
    }

    @Override
    protected Void doInBackground(Void... params) {
        PackageInfo info;
        try {
            info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        int currentVersion = info.versionCode;
        mVersionName = info.versionName;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int lastVersion = prefs.getInt(VERSION_CODE, 0);

        // If the app has updated to a newer version from a older version we will check for upgrades.
        if (currentVersion > lastVersion) {
            if (BuildConfig.DEBUG) {
                mRemoteLogger.d("Updating to " + mVersionName + " - " + currentVersion);
            }
            // Run all required methods.
            for (int i = lastVersion; i <= currentVersion; i++) {
                runMethod(i);
            }
        }
        if (succesfulMigrate) {
            prefs.edit().putInt(VERSION_CODE, currentVersion).apply();
        }
        return null;
    }

    public static boolean requiresUpdate(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int lastVersion = PreferenceManager.getDefaultSharedPreferences(context).getInt(VERSION_CODE, 0);
            if (info.versionCode != lastVersion) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Void result) {
        mProgressDialog.dismiss();

        mListener.OnUpdateCompleted();
    }

    /**
     * Method to update application by given versionNumber.
     *
     * @param version
     */
    private void runMethod(int version) {
        switch (version) {
            case v2_1_1:
                setSipEnabled();
                break;
            case v4_0:
                migrateCredentials();
                break;
            case v5_2:
                enableSecureCalling();
                break;
        }
    }

    /**
     * Migrate the default settings for sip permissions.
     * V2_2_1
     */
    private void setSipEnabled() {
        if (mPreferences.hasPhoneAccount() && mPreferences.hasSipPermission()) {
            PhoneAccountHelper phoneAccountHelper = new PhoneAccountHelper(mContext);
            phoneAccountHelper.savePhoneAccountAndRegister(
                    (PhoneAccount) mJsonStorage.get(PhoneAccount.class));
        }
    }

    /**
     * Migrate credentials from systemuser to Account.
     * V4_0
     */
    private void migrateCredentials() {
        SystemUser user = (SystemUser) mJsonStorage.get(SystemUser.class);
        if (user != null && user.getPassword() != null) {
            new AccountHelper(mContext).setCredentials(user.getEmail(), user.getPassword());
            // Cleanup.
            user.setPassword(null);
        }
    }

    /**
     * Migrate secure calling permissions to use secure calling by default.
     * V5_2
     */
    private void enableSecureCalling() {
        AccountHelper accountHelper = new AccountHelper(mContext);

        Api mApi = ServiceGenerator.createService(
                mContext,
                Api.class,
                mContext.getString(R.string.api_url),
                accountHelper.getEmail(),
                accountHelper.getPassword()
        );
        Call<UseEncryption> call = mApi.useEncryption(new UseEncryption(true));
        call.enqueue(this);
    }

    @Override
    public void onResponse(Call<UseEncryption> call, Response<UseEncryption> response) {
        if (!response.isSuccessful()) {
            succesfulMigrate = false;
        }
    }

    @Override
    public void onFailure(Call<UseEncryption> call, Throwable t) {
        mRemoteLogger.d("Setting secure calling failed.");
        succesfulMigrate = false;
    }
}

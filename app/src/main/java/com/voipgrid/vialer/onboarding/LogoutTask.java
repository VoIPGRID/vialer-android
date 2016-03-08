package com.voipgrid.vialer.onboarding;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.voipgrid.vialer.util.Middleware;

import java.io.IOException;

import retrofit.RetrofitError;

/**
 * LogoutTask can be executed to unregister in the middleware in a seperate thread.
 */
public class LogoutTask extends AsyncTask{

    private static final String LOG_TAG = LogoutTask.class.getSimpleName();
    private Context mContext;

    public LogoutTask(Context context) {
        mContext = context;
    }

    @Override
    protected Object doInBackground(Object[] params) {

        try{
            Middleware.unregister(mContext);
        }
        catch (IOException exception) {
            Log.e(LOG_TAG, "Deregistration failed", exception);
        }
        return null;
    }
}

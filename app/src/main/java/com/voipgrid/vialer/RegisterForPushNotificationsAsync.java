package com.voipgrid.vialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.voipgrid.vialer.middleware.MiddlewareHelper;

import java.net.URL;

import me.pushy.sdk.Pushy;

class RegisterForPushNotificationsAsync extends AsyncTask<Void, Void, Exception> {
    private Context mContext;

    public RegisterForPushNotificationsAsync(Context context) {

        mContext = context;
    }

    protected Exception doInBackground(Void... params) {
        try {
            // Assign a unique token to this device
            String deviceToken = Pushy.register(mContext);

            // Log it for debugging purposes
            Log.d("MyApp", "Pushy device token: " + deviceToken);

            MiddlewareHelper.setPushyToken(mContext,deviceToken);
        }
        catch (Exception exc) {
            // Return exc to onPostExecute
            return exc;
        }

        // Success
        return null;
    }

    @Override
    protected void onPostExecute(Exception exc) {
        // Failed?
        if (exc != null) {
            // Show error as toast message
            Toast.makeText(mContext, exc.toString(), Toast.LENGTH_LONG).show();
            return;
        }

        // Succeeded, optionally do something to alert the user
    }
}
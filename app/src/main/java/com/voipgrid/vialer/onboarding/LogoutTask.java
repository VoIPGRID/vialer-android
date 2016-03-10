package com.voipgrid.vialer.onboarding;

import android.content.Context;
import android.os.AsyncTask;

import com.voipgrid.vialer.util.Middleware;


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
//        Middleware.unregister(mContext);
        return null;
    }
}

package com.voipgrid.vialer.util;

import static java.lang.String.format;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.voipgrid.vialer.R;

public class UserAgent {

    private Context mContext;

    public UserAgent(Context context) {
        mContext = context;
    }

    /**
     * Generates the user agent based on various information about the application.
     *
     * @return The user agent as a string
     */
    public String generate() {
        String appName = mContext.getString(R.string.app_name);
        String version = "?";
        try {
            PackageInfo packageInfo = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return format("%s/%s (Android; %s, %s %s)", appName, version, Build.VERSION.RELEASE, Build.MANUFACTURER, Build.PRODUCT);
    }
}

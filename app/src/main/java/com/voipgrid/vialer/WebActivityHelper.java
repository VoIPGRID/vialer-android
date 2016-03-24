package com.voipgrid.vialer;

import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.JsonStorage;

/**
 * Created by stefan on 22-3-16.
 */
public class WebActivityHelper {

    Context mContext;

    public WebActivityHelper(Context context){
        mContext = context;
    }

    /**
     * Start a new WebActivity to display the page
     * @param title
     * @param page
     */
    public void startWebActivity(String title, String page) {
        SystemUser systemUser = (SystemUser) new JsonStorage(mContext).get(SystemUser.class);
        Intent intent = new Intent(mContext, WebActivity.class);
        intent.putExtra(WebActivity.PAGE, page);
        intent.putExtra(WebActivity.TITLE, title);
        intent.putExtra(WebActivity.USERNAME, systemUser.getEmail());
        intent.putExtra(WebActivity.PASSWORD, systemUser.getPassword());
        mContext.startActivity(intent);
    }
}

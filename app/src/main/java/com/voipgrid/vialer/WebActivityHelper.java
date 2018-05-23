package com.voipgrid.vialer;

import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.util.AccountHelper;

/**
 * Used to start a Web Activity from anywhere in the application.
 */
public class WebActivityHelper {

    Context mContext;

    public WebActivityHelper(Context context){
        mContext = context;
    }

    public void startWebActivity(String title, String page) {
        startWebActivity(title, page, null);
    }

    /**
     * Start a new WebActivity to display the page.
     * @param title
     * @param page
     */
    public void startWebActivity(String title, String page, String gaTitle) {
        AccountHelper accountHelper = new AccountHelper(mContext);
        Intent intent = new Intent(mContext, WebActivity.class);
        intent.putExtra(WebActivity.PAGE, page);
        intent.putExtra(WebActivity.TITLE, title);
        intent.putExtra(WebActivity.USERNAME, accountHelper.getEmail());
        intent.putExtra(WebActivity.PASSWORD, accountHelper.getPassword());
        intent.putExtra(WebActivity.API_TOKEN, accountHelper.getApiToken());
        if (gaTitle != null) {
            intent.putExtra(WebActivity.GA_TITLE, gaTitle);
        }
        mContext.startActivity(intent);
    }
}

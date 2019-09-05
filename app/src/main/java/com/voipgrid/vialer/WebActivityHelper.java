package com.voipgrid.vialer;

import android.content.Context;
import android.content.Intent;

/**
 * Used to start a Web Activity from anywhere in the application.
 */
public class WebActivityHelper {

    Context mContext;

    public WebActivityHelper(Context context){
        mContext = context;
    }

    /**
     * Start a new WebActivity to display the page.
     * @param title
     * @param page
     */
    public void startWebActivity(String title, String page, String gaTitle) {
        Intent intent = new Intent(mContext, WebActivity.class);
        intent.putExtra(WebActivity.PAGE, page);
        intent.putExtra(WebActivity.TITLE, title);
        if (gaTitle != null) {
            intent.putExtra(WebActivity.GA_TITLE, gaTitle);
        }
        mContext.startActivity(intent);
    }
}

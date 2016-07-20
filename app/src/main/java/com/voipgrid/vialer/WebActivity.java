package com.voipgrid.vialer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.AutoLoginToken;
import com.voipgrid.vialer.util.LoginRequiredActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Activity to display the web pages within the app.
 */
public class WebActivity extends LoginRequiredActivity implements Callback<AutoLoginToken> {

    public static final String PAGE = "key-page";
    public static final String TITLE = "key-title";
    public static final String USERNAME = "key-username";
    public static final String PASSWORD = "key-password";
    public static final String GA_TITLE = "ga-title";

    private ProgressBar mProgressBar;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        /* set the Toolbar to use as ActionBar */
        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));

        /* set the Toolbar title */
        getSupportActionBar().setTitle(getIntent().getStringExtra(TITLE));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);

        /* get the ProgressBar */
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        /* get the WebView */
        mWebView = (WebView) findViewById(R.id.web_view);

        /* enable javascript */
        mWebView.getSettings().setJavaScriptEnabled(true);

        /* set webview client */
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                webView.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.INVISIBLE);
                super.onPageFinished(view, url);
            }
        });

        String uri = getIntent().getStringExtra(PAGE);
        if(uri.startsWith("http")) {
            loadPage(uri);
        } else {
            /* request an autologin token and load the requested page */
            autoLoginToken();
        }

        // Track the web view.
        AnalyticsHelper analyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );
        analyticsHelper.sendScreenViewTrack(getIntent().getStringExtra(GA_TITLE));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    /**
     * Request an autologin token
     */
    private void autoLoginToken() {
        mProgressBar.setVisibility(View.VISIBLE);
        Api api = ServiceGenerator.createService(
                this,
                Api.class,
                getString(R.string.api_url),
                getIntent().getStringExtra(USERNAME),
                getIntent().getStringExtra(PASSWORD)
        );
        Call<AutoLoginToken> call = api.autoLoginToken();
        call.enqueue(this);
    }

    /**
     * Load the page for the given url in the webview
     * @param url
     */
    private void loadPage(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
    }

    @Override
    public void onResponse(Call<AutoLoginToken> call, Response<AutoLoginToken> response) {
        String username = getIntent().getStringExtra(USERNAME);

        if (response.isSuccess() && response.body() != null) {
            AutoLoginToken autoLoginToken = response.body();
            try {
                username = URLEncoder.encode(username, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            loadPage(getString(
                            R.string.web_autologin,
                            getString(R.string.web_url),
                            username,
                            autoLoginToken,
                            getIntent().getStringExtra(PAGE)
                    )
            );
        } else {
            failedFeedback(getString(R.string.webactivity_open_page_failed));
        }
    }

    @Override
    public void onFailure(Call<AutoLoginToken> call, Throwable t) {
        failedFeedback(getString(R.string.webactivity_open_page_failed));
    }

    private void failedFeedback(String message) {
        final String mMessage = message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.auto_login_error, mMessage),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}

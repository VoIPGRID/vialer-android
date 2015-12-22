package com.voipgrid.vialer;

import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.AutoLoginToken;
import com.voipgrid.vialer.util.ConnectivityHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import retrofit.RetrofitError;
import retrofit.client.OkClient;

/**
 * Activity to display the web pages within the app.
 */
public class WebActivity extends AppCompatActivity implements retrofit.Callback<AutoLoginToken> {

    public static final String PAGE = "key-page";
    public static final String TITLE = "key-title";
    public static final String USERNAME = "key-username";
    public static final String PASSWORD = "key-password";

    private ProgressBar mProgressBar;
    private WebView mWebView;

    private ConnectivityHelper mConnectivityHelper;

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

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

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
    }

    /**
     * Request an autologin token
     */
    private void autoLoginToken() {
        mProgressBar.setVisibility(View.VISIBLE);
        Api api = ServiceGenerator.createService(mConnectivityHelper, Api.class,
                getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(
                        this,
                        getIntent().getStringExtra(USERNAME),
                        getIntent().getStringExtra(PASSWORD)))
        );
        api.autoLoginToken(this);
    }

    /**
     * Received an autologin token, now we can load the requested page with the received token
     * @param autoLoginToken
     * @param response
     */
    @Override
    public void success(AutoLoginToken autoLoginToken, retrofit.client.Response response) {
        String username = getIntent().getStringExtra(USERNAME);
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
    }

    /**
     * Failed to receive an autologin token. Show an error message to the user
     * @param error
     */
    @Override
    public void failure(RetrofitError error) {
        final String errorMessage = error.getMessage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.auto_login_error, errorMessage),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

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
}

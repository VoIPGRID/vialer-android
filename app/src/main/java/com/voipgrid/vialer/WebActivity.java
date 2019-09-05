package com.voipgrid.vialer;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.voipgrid.vialer.api.VoipgridApi;
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
    public static final String API_TOKEN = "key-api-token";
    public static final String GA_TITLE = "ga-title";

    private ProgressBar mProgressBar;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        // Set the Toolbar to use as ActionBar
        setSupportActionBar(findViewById(R.id.action_bar));

        // Set the Toolbar title
        getSupportActionBar().setTitle(getIntent().getStringExtra(TITLE));

        // Enabled home as up for the Toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Enabled home button for the Toolbar
        getSupportActionBar().setHomeButtonEnabled(true);

        // Get the ProgressBar
        mProgressBar = findViewById(R.id.progress_bar);

        // Get the WebView.
        mWebView = findViewById(R.id.web_view);

        // Enable javascript.
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.getSettings().setDomStorageEnabled(true);

        // Set webview client.
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
                super.onPageFinished(view, url);

                mProgressBar.setVisibility(View.INVISIBLE);
            }
        });

        String uri = getIntent().getStringExtra(PAGE);
        if(uri.startsWith("http")) {
            loadPage(uri);
        } else {
            // request an autologin token and load the requested page.
            autoLoginToken();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        finish();
        return true;
    }

    /**
     * Request an autologin token
     */
    private void autoLoginToken() {
        mProgressBar.setVisibility(View.VISIBLE);
        VoipgridApi voipgridApi = ServiceGenerator.createApiService(this);
        Call<AutoLoginToken> call = voipgridApi.autoLoginToken();
        call.enqueue(this);
    }

    /**
     * Load the page for the given url in the webview
     * @param url
     */
    private void loadPage(final String url) {
        runOnUiThread(() -> mWebView.loadUrl(url));
    }

    @Override
    public void onResponse(@NonNull Call<AutoLoginToken> call, @NonNull Response<AutoLoginToken> response) {
        String username = "";

        if (response.isSuccessful() && response.body() != null) {
            AutoLoginToken autoLoginToken = response.body();
            try {
                username = URLEncoder.encode(User.getUsername(), "utf-8");
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
    public void onFailure(@NonNull Call<AutoLoginToken> call, @NonNull Throwable t) {
        failedFeedback(getString(R.string.webactivity_open_page_failed));
    }

    private void failedFeedback(String message) {
        final String mMessage = message;
        runOnUiThread(() -> {
            mProgressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.auto_login_error, mMessage),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}

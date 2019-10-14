package com.voipgrid.vialer;

import android.content.Context;
import android.content.Intent;
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
public class VoIPGRIDPortalWebActivity extends LoginRequiredActivity implements Callback<AutoLoginToken> {

    public static final String PAGE = "key-page";
    public static final String TITLE = "key-title";

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

    public static void launchForPasswordChange(Context context) {
        launch(context, R.string.password_change_title, R.string.web_password_change);
    }

    public static void launchForUserDestinations(Context context) {
        launch(context, R.string.add_destination_title, R.string.web_add_destination);
    }

    public static void launchForStats(Context context) {
        launch(context, R.string.statistics_menu_item_title, R.string.web_statistics);
    }

    public static void launchForDialPlan(Context context) {
        launch(context, R.string.dial_plan_menu_item_title, R.string.web_dial_plan);
    }

    public static void launchForAppInfo(Context context) {
        launch(context, R.string.info_menu_item_title, R.string.url_app_info);
    }

    public static void launchForChangeUser(Context context) {
        launch(context, R.string.user_change_title, R.string.web_user_change);
    }

    public static void launchForChangePassword(Context context) {
        launch(context, R.string.password_change_title, R.string.web_password_change);
    }

    private static void launch(Context context, int title, int page) {
        Intent intent = new Intent(context, VoIPGRIDPortalWebActivity.class);
        intent.putExtra(VoIPGRIDPortalWebActivity.TITLE, context.getString(title));
        intent.putExtra(VoIPGRIDPortalWebActivity.PAGE, context.getString(page));
        context.startActivity(intent);
    }
}

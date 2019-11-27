package com.voipgrid.vialer.voipgrid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.fillField
import com.voipgrid.vialer.logging.VialerBaseActivity
import kotlinx.android.synthetic.main.activity_create_new_password.*
import kotlinx.android.synthetic.main.activity_web.web_view
import javax.inject.Inject

class PasswordResetWebActivity : VialerBaseActivity() {

    @Inject lateinit var voipgridApi: VoipgridApi

    val username: String
        get() = intent.getStringExtra(USERNAME_EXTRA) ?: ""

    val password: String
        get() = intent.getStringExtra(PASSWORD_EXTRA) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)

        setContentView(R.layout.activity_create_new_password)

        button_continue.setOnClickListener {
            hideKeyboard()
            loadPortal(username, password, password_repeat_edit_text.text.toString())
        }

        password_edit_text.addTextChangedListener(TextChangedListener())
        password_repeat_edit_text.addTextChangedListener(TextChangedListener())
        button_continue.isEnabled = userHasEnteredValidMatchingPasswords()
    }

    private fun userHasEnteredValidMatchingPasswords() : Boolean {
        val password1 = password_edit_text.text.toString()
        val password2 = password_repeat_edit_text.text.toString()

        when {
            password1 != password2  -> return false
            password2.length < 6    -> return false
            password2 == password   -> return false
        }

        return password2.contains(Regex("[^a-zA-Z]"))
    }

    private fun loadPortal(username: String, password: String, newPassword: String) {
        password_container.visibility = GONE
        web_view.apply {
            visibility = View.VISIBLE
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (!url.contains("user/login/")){
                        setResult(1, Intent().apply {
                            putExtra(USERNAME_EXTRA, username)
                            putExtra(PASSWORD_EXTRA, newPassword)
                        })
                        finish()
                        return
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.fillField("id_auth-username", username)
                    view.fillField("id_auth-password", password)
                    view.fillField("id_change-old_password", password)
                    view.fillField("id_change-new_password1", newPassword)
                    view.fillField("id_change-new_password2", newPassword)
                }
            }
            loadUrl(getString(PAGE_URL))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        WebStorage.getInstance().deleteAllData()
        web_view.apply {
            clearCache(true)
            clearHistory()
            destroy()
        }
    }

    inner class TextChangedListener : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            button_continue.isEnabled = userHasEnteredValidMatchingPasswords()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

    companion object {
        const val PAGE_URL = R.string.web_password_change
        const val USERNAME_EXTRA = "USERNAME_EXTRA"
        const val PASSWORD_EXTRA = "PASSWORD_EXTRA"
        const val REQUEST_CODE = 533

        fun launch(activity: Activity, username: String, password: String) {
            activity.startActivityForResult(Intent(activity, PasswordResetWebActivity::class.java).apply {
                putExtra(USERNAME_EXTRA, username)
                putExtra(PASSWORD_EXTRA, password)
            }, REQUEST_CODE)
        }
    }
}
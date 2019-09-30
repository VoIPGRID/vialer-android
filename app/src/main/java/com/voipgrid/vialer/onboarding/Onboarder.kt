package com.voipgrid.vialer.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.VialerBaseActivity
import com.voipgrid.vialer.onboarding.core.OnboardingState
import kotlinx.android.synthetic.main.activity_onboarding.*

typealias PermissionCallback = () -> Unit

abstract class Onboarder : VialerBaseActivity() {

    protected val logger = Logger(this)

    private var permissionCallback: PermissionCallback? = null

    open val state: OnboardingState = OnboardingState()

    var isLoading: Boolean
        get() = progress.visibility == VISIBLE
        set(loading) {
            progress.visibility = if (loading) VISIBLE else INVISIBLE
        }

    abstract fun progress()

    abstract fun restart()

    abstract override fun onBackPressed()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
    }

    /**
     * Request a permission and provide a block that will be called back when the
     * result is received. Only one callback can be active at any one time.
     *
     */
    fun requestPermission(permission: String, callback: PermissionCallback) {
        logger.i("Requesting $permission")
        permissionCallback = callback
        ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
    }

    /**
     * Invoke the permission callback when the user accepts/denies a permission.
     *
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        logger.i("Received permission result, invoking callback")
        permissionCallback?.invoke()
    }

    /**
     * Whenever we receive an activity result, we want to invoke the permission callback
     * as this is used when requesting the app to be whitelisted.
     *
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logger.i("Received activity result, invoking callback")
        permissionCallback?.invoke()
    }

    /**
     * Hides the keyboard, this should be called every time a new fragment is loaded.
     *
     */
    protected fun hideKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        if (currentFocus != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }
}


package com.voipgrid.vialer.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.OnboardingState
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.voipgrid.PasswordResetWebActivity
import kotlinx.android.synthetic.main.activity_onboarding.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import com.voipgrid.vialer.logging.VialerBaseActivity
import com.voipgrid.vialer.voipgrid.PasswordResetWebActivity.Companion.PASSWORD_EXTRA
import com.voipgrid.vialer.voipgrid.PasswordResetWebActivity.Companion.USERNAME_EXTRA


typealias PermissionCallback = () -> Unit

abstract class Onboarder : VialerBaseActivity() {

    override val logger = Logger(this).forceRemoteLogging(true)

    private var permissionCallback: PermissionCallback? = null

    open val state: OnboardingState = OnboardingState()

    var isLoading: Boolean
        get() = progress.visibility == VISIBLE
        set(loading) {
            progress.visibility = if (loading) VISIBLE else INVISIBLE
        }

    abstract fun progress(callerStep: Step)

    abstract fun restart()

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
        setCallBack(callback)
        ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
    }

    fun setCallBack(callback: PermissionCallback) {
        permissionCallback = callback
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
        if (requestCode == PasswordResetWebActivity.REQUEST_CODE) {
            finish()
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                putExtra(USERNAME_EXTRA, data?.getStringExtra(USERNAME_EXTRA))
                putExtra(PASSWORD_EXTRA, data?.getStringExtra(PASSWORD_EXTRA))
            })
            return
        }

        logger.i("Received activity result, invoking callback")
        permissionCallback?.invoke()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }

}
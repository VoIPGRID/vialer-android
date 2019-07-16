package com.voipgrid.vialer.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.voipgrid.vialer.Logout
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.onboarding.steps.*
import com.voipgrid.vialer.onboarding.steps.permissions.ContactsPermissionStep
import com.voipgrid.vialer.onboarding.steps.permissions.MicrophonePermissionStep
import com.voipgrid.vialer.onboarding.steps.permissions.OptimizationWhitelistStep
import com.voipgrid.vialer.onboarding.steps.permissions.PhoneStatePermissionStep
import kotlinx.android.synthetic.main.activity_onboarding.*
import javax.inject.Inject

typealias PermissionCallback = () -> Unit

class OnboardingActivity: AppCompatActivity() {

    @Inject lateinit var logout: Logout

    private lateinit var adapter: OnboardingAdapter
    private val logger = Logger(this)

    private var permissionCallback: PermissionCallback? = null

    var username = ""
    var password = ""
    var code = ""
    var requiresTwoFactor = false
    var hasVoipAccount = true

    private val currentStep : Step
        get() = adapter.getStep(viewPager.currentItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        VialerApplication.get().component().inject(this)

        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())

        adapter = OnboardingAdapter(supportFragmentManager, lifecycle).apply {
            addStep(LogoStep())
            addStep(LoginStep())
            addStep(TwoFactorStep())
            addStep(MobileNumberStep())
            addStep(MissingVoipAccountStep())
            addStep(ContactsPermissionStep())
            addStep(PhoneStatePermissionStep())
            addStep(MicrophonePermissionStep())
            addStep(OptimizationWhitelistStep())
            addStep(WelcomeStep())
        }

        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
    }

    fun progress() {
        if (isLastItem()) {
            logger.i("Onboarding has been completed, forwarding to the main activity")

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        logger.i("Progressing the onboarder from ${currentStep.javaClass.simpleName}")

        runOnUiThread {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }
    }

    override fun onBackPressed() {
        restart()
    }

    private fun isLastItem(): Boolean {
        return viewPager.currentItem == (adapter.itemCount - 1)
    }

    /**
     * Restart the onboarding process.
     *
     */
    fun restart() {
        logout.perform(true)
        finish()
        startActivity(intent)
    }

    /**
     * Request a permission and provide a block that will be called back when the
     * result is received. Only one callback can be active at any one time.
     *
     */
    fun requestPermission(permission: String, callback: PermissionCallback) {
        permissionCallback = callback
        ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionCallback?.invoke()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionCallback?.invoke()
    }

    private fun hideKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        if (currentFocus != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    private inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(currentPage: Int) {
            super.onPageSelected(currentPage)
            hideKeyboard()

            viewPager.postDelayed({
                val currentStep = adapter.getStep(currentPage)
                val nextPage = currentPage + 1

                if (currentStep.shouldThisStepBeSkipped()) {
                    logger.i("Skipping ${currentStep.javaClass.simpleName} at {$currentPage} and moving to {$nextPage}")
                    viewPager.setCurrentItem(nextPage, false)
                }
            }, 10)
        }
    }
}


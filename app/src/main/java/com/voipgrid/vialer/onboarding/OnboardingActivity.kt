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
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.steps.*
import com.voipgrid.vialer.onboarding.steps.permissions.ContactsPermissionStep
import com.voipgrid.vialer.onboarding.steps.permissions.MicrophonePermissionStep
import com.voipgrid.vialer.onboarding.steps.permissions.OptimizationWhitelistStep
import com.voipgrid.vialer.onboarding.steps.permissions.PhoneStatePermissionStep
import kotlinx.android.synthetic.main.activity_onboarding.*

typealias PermissionCallback = () -> Unit

class OnboardingActivity: AppCompatActivity() {

    private lateinit var adapter: OnboardingAdapter
    private val logger = Logger(this)

    private var permissionCallback: PermissionCallback? = null

    var username = ""
    var password = ""
    var code = ""
    var requiresTwoFactor = false

    val currentStep : Step
        get() = adapter.getStep(viewPager.currentItem)

    private val nextStep : Step
        get() = adapter.getStep(viewPager.currentItem + 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())

        adapter = OnboardingAdapter(supportFragmentManager, lifecycle).apply {
            addStep(LogoStep())
            addStep(LoginStep())
            addStep(TwoFactorStep())
            addStep(MobileNumberStep())
            addStep(ContactsPermissionStep())
            addStep(PhoneStatePermissionStep())
            addStep(MicrophonePermissionStep())
            addStep(OptimizationWhitelistStep())
            addStep(WelcomeStep())
        }

        viewPager.adapter = adapter
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

        logger.i("Progressing the onboarder from ${currentStep.javaClass.simpleName} to ${nextStep.javaClass.simpleName}")

        viewPager.setCurrentItem(viewPager.currentItem + 1, true)
    }

    private fun isLastItem(): Boolean {
        return viewPager.currentItem == (adapter.itemCount - 1)
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

    private inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            if (currentFocus != null) {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            }

            viewPager.isUserInputEnabled = adapter.getStep(position).canManuallyLeaveThisStep


            if (adapter.getStep(position).shouldThisStepBeSkipped()) {
                logger.i("Skipping the current ${currentStep.javaClass.simpleName} and moving to ${nextStep.javaClass.simpleName}")

                viewPager.setCurrentItem(position, false)
            }
        }
    }
}


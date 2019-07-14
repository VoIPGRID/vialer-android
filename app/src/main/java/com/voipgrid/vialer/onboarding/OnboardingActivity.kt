package com.voipgrid.vialer.onboarding

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.steps.LoginStep
import com.voipgrid.vialer.onboarding.steps.PermissionsStep
import kotlinx.android.synthetic.main.activity_onboarding.*

class OnboardingActivity: AppCompatActivity() {

    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        adapter = OnboardingAdapter(supportFragmentManager, lifecycle)
        adapter.addStep(LoginStep())
        adapter.addStep(PermissionsStep("call", "we use call permission cause this phone", "call_permission"))
        adapter.addStep(PermissionsStep("contacts", "we use contacts", "contact_permission"))

        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())
    }

    override fun onBackPressed() {
        viewPager.setCurrentItem(1, true)
    }

    private inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

            viewPager.isUserInputEnabled = adapter.getStep(position).canManuallyLeaveThisStep
        }
    }
}


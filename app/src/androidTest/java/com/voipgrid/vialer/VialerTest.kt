package com.voipgrid.vialer

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
abstract class VialerTest {

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)

    protected fun enterMobileNumber(number: String = "+447123463890") {
        onView(withId(R.id.mobileNumberTextDialog)).perform(replaceText(""))
        onView(withId(R.id.mobileNumberTextDialog)).perform(typeText(number))
        onView(withId(R.id.button_configure)).check(matches(ViewMatchers.isEnabled()))
        onView(withId(R.id.button_configure)).perform(click())
        Thread.sleep(2000)
    }

    protected fun login(username: String = BuildConfig.TEST_USERNAME, password: String = BuildConfig.TEST_PASSWORD) {
        check(username.isNotEmpty()) { "Test username not set" }
        check(password.isNotEmpty()) { "Test password not set" }

        onView(withId(R.id.emailTextDialog)).perform(typeText(username))
        onView(withId(R.id.passwordTextDialog)).perform(typeText(password))
        onView(withId(R.id.button_login)).perform(click())
        Thread.sleep(2000)
    }

     fun onboard(skipIfLoggedIn: Boolean = false): Boolean {
         Thread.sleep(2000)
        try {
            onView(withId(R.id.emailTextDialog)).check(matches(isDisplayed()))
        } catch (e: Exception) {
            if (skipIfLoggedIn) {
                return false
            } else {
                throw e
            }
        }

        login()
        enterMobileNumber()
        onView(ViewMatchers.withText("Battery Optimization")).check(matches(isDisplayed()))
        onView(withId(R.id.denyButton)).perform(click())
        Thread.sleep(5000)
        onView(ViewMatchers.withText("ALL CALLS")).check(matches(isDisplayed()))
        return true
    }
}
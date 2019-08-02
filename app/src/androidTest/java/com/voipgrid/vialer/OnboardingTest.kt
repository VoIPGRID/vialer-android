package com.voipgrid.vialer


import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.GrantPermissionRule
import com.voipgrid.vialer.onboarding.OnboardingActivity
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test

class OnboardingTest : VialerTest() {

    @Rule
    @JvmField
    var mActivityTestRule = VialerActivityTestRule(OnboardingActivity::class.java, resetStateAfterEachTest = true)

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)

    @Test
    fun loginFailsWithInvalidCredentials() {
        onView(withText("calls in the cloud")).check(matches(isDisplayed()))
        onView(withId(R.id.emailTextDialog)).perform(typeText("Invalid email"))
        onView(withId(R.id.passwordTextDialog)).perform(typeText("Invalid password"))
        onView(withId(R.id.button_login)).perform(click())

        Thread.sleep(2000)
        onView(withText("Login failed")).check(matches(isDisplayed()))
    }

    @Test
    fun loginButtonCannotBeClickedIfEmailAddressIsNotFilledIn() {
        onView(withId(R.id.passwordTextDialog)).perform(typeText("apassword"))
        onView(withId(R.id.button_login)).check(matches(not(isEnabled())))
    }

    @Test
    fun loginButtonCannotBeClickedIfPasswordIsNotFilledIn() {
        onView(withId(R.id.emailTextDialog)).perform(typeText("anemail"))
        onView(withId(R.id.button_login)).check(matches(not(isEnabled())))
    }

    @Test
    fun itCanLoginWithCorrectCredentials() {
        login()
        onView(withText("Vialer is configuring")).check(matches(isDisplayed()))
    }

    @Test
    fun itWillNotAllowConfigurationWithoutAMobileNumber() {
        login()
        onView(withId(R.id.mobileNumberTextDialog)).perform(replaceText(""))
        onView(withId(R.id.button_configure)).check(matches(not(isEnabled())))
    }

    @Test
    fun itWillAcceptAValidMobileNumber() {
        login()
        enterMobileNumber()
        Thread.sleep(2000)
        onView(withId(R.id.button_configure)).check(doesNotExist())
    }

    @Test
    fun itSkipsPermissionsAndFinishedOnboarding() {
        login()
        enterMobileNumber()
        onView(withText("Battery Optimization")).check(matches(isDisplayed()))
        onView(withId(R.id.denyButton)).perform(click())
        Thread.sleep(5000)
        onView(withText("MY CALLS")).check(matches(isDisplayed()))
    }

    private fun enterMobileNumber(number: String = "+447123463890") {
        onView(withId(R.id.mobileNumberTextDialog)).perform(replaceText(""))
        onView(withId(R.id.mobileNumberTextDialog)).perform(typeText(number))
        onView(withId(R.id.button_configure)).check(matches(isEnabled()))
        onView(withId(R.id.button_configure)).perform(click())
        Thread.sleep(2000)
    }

    private fun login(username: String = BuildConfig.TEST_USERNAME, password: String = BuildConfig.TEST_PASSWORD) {
        onView(withId(R.id.emailTextDialog)).perform(typeText(username))
        onView(withId(R.id.passwordTextDialog)).perform(typeText(password))
        onView(withId(R.id.button_login)).perform(click())
        Thread.sleep(2000)
    }
}

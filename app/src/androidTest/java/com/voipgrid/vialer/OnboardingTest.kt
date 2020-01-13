package com.voipgrid.vialer


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.runner.AndroidJUnit4
import com.voipgrid.vialer.onboarding.OnboardingActivity
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTest : VialerTest() {

    @Rule
    @JvmField
    var rule = VialerActivityTestRule(OnboardingActivity::class.java, this, resetStateAfterEachTest = true, loginAutomatically = false)

    @Test
    fun loginFailsWithInvalidCredentials() {
        login(username = "Invalid email", password = "Invalid password");
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
        onView(withText("Enter your mobile phone number below:")).check(matches(isDisplayed()))
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
        onboard()
    }
}

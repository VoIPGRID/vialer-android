package com.voipgrid.vialer


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class LoginTest : VialerTest() {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

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
        onView(withId(R.id.emailTextDialog)).perform(typeText(BuildConfig.TEST_USERNAME))
        onView(withId(R.id.passwordTextDialog)).perform(typeText(BuildConfig.TEST_PASSWORD))
        onView(withId(R.id.button_login)).perform(click())

        Thread.sleep(2000)
        onView(withText("Vialer is configuring")).check(matches(isDisplayed()))
    }

}

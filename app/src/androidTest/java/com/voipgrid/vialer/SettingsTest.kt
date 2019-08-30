package com.voipgrid.vialer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest : VialerTest() {

    @Rule
    @JvmField
    var rule = VialerActivityTestRule(SettingsActivity::class.java, this, resetStateAfterEachTest = false)

    @Test
    fun voipAccountTogglesWhenVoipIsToggled() {
        Thread.sleep(2000)
        onView(withId(R.id.account_sip_id_edit_text)).check(matches(isDisplayed()))
        onView(withId(R.id.account_sip_switch)).perform(click())
        Thread.sleep(2000)
        onView(withId(R.id.account_sip_id_edit_text)).check(matches(not(isDisplayed())))
        onView(withId(R.id.account_sip_switch)).perform(click())
    }

    @Test
    fun remoteLoggingIdIsShownWhenEnabled() {
        Thread.sleep(2000)
        onView(withId(R.id.remote_logging_switch)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.remote_logging_id_edit_text)).check(matches(not(withText(""))))
    }
}
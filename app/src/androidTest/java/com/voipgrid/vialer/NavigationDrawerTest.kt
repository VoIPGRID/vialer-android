package com.voipgrid.vialer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test

class NavigationDrawerTest : VialerTest() {

    @Rule
    @JvmField
    var rule = VialerActivityTestRule(MainActivity::class.java, this, resetStateAfterEachTest = false)

    @Test
    fun drawerCanBeOpenedAtSettingsAccessed() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        Thread.sleep(1000)
        onView(withText("Settings")).perform(click())
        Thread.sleep(1000)
        onView(withText("Call using VoIP")).check(matches(isDisplayed()))
    }
}
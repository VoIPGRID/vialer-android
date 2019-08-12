package com.voipgrid.vialer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.voipgrid.vialer.onboarding.OnboardingActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CallTest : VialerTest() {

    @Rule
    @JvmField
    var rule = VialerActivityTestRule(OnboardingActivity::class.java, resetStateAfterEachTest = false)

    @Before
    fun loginToApp() {
        onboard()
    }

    @Test
    fun itWillPlaceACall() {
        onView(withId(R.id.floating_action_button)).perform(click())
        Thread.sleep(1000)
        dialNumberOnKeypad(BuildConfig.TEST_END_NUMBER)
        onView(withId(R.id.button_call)).perform(click())
        Thread.sleep(5000)
        onView(withId(R.id.button_mute)).check(matches(isEnabled()))
        onView(withId(R.id.button_hangup)).perform(click())
        Thread.sleep(6000)
        onView(withText("MY CALLS")).check(matches(isDisplayed()))
    }

    private fun dialNumberOnKeypad(number: String) {
        number.forEach {
            when(it) {
                '*' -> R.id.dialpadButtonAsterisk
                '#' -> R.id.dialpadButtonHash
                '1' -> R.id.dialpadButton1
                '2' -> R.id.dialpadButton2
                '3' -> R.id.dialpadButton3
                '4' -> R.id.dialpadButton4
                '5' -> R.id.dialpadButton5
                '6' -> R.id.dialpadButton6
                '7' -> R.id.dialpadButton7
                '8' -> R.id.dialpadButton8
                '9' -> R.id.dialpadButton9
                '0' -> R.id.dialpadButton0
                '+' -> -1
                else -> null
            }?.let { button ->
                if (button == -1) {
                    onView(withId(R.id.dialpadButton0)).perform(longClick())
                } else {
                    onView(withId(button)).perform(click())
                }
            }
        }
    }

}
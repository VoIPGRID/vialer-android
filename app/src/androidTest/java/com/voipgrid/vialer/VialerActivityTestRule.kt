package com.voipgrid.vialer

import android.app.Activity
import android.preference.PreferenceManager
import androidx.test.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule

/**
 * For testing an activity, if true is passed, state (shared preferences) will be wiped after each
 * test.
 *
 */
class VialerActivityTestRule<A : Activity>(a: Class<A>, private val vialerTest: VialerTest, val resetStateAfterEachTest: Boolean = false, private val loginAutomatically: Boolean = true) : ActivityTestRule<A>(a) {

    override fun afterActivityLaunched() {
        super.afterActivityLaunched()
        Thread.sleep(1000)
        if (!loginAutomatically) return

        if (vialerTest.onboard(skipIfLoggedIn = true)) {
            launchActivity(activity.intent)
        }
    }

    override fun afterActivityFinished() {
        super.afterActivityFinished()

        if (!resetStateAfterEachTest) return
        
        InstrumentationRegistry.getInstrumentation()?.targetContext?.let {
            val name = PreferenceManager.getDefaultSharedPreferencesName(it)
            it.deleteSharedPreferences(name)
        }
    }
}
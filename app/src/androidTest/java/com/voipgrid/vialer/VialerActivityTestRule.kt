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
class VialerActivityTestRule<A : Activity>(a: Class<A>, val resetStateAfterEachTest: Boolean = false) : ActivityTestRule<A>(a) {

    override fun afterActivityLaunched() {
        super.afterActivityLaunched()
        Thread.sleep(1000)
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
package com.voipgrid.vialer;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.voipgrid.vialer.onboarding.SetupActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OnBoardingLogoTest {

    @Rule
    public ActivityTestRule<SetupActivity> mActivityTestRule = new ActivityTestRule<>(SetupActivity.class);

    private TestActions mActions;

    @Before
    public void setUp() {
        mActions = new TestActions();
    }

    @Test
    public void testClickProceedFromLogo_sameActivity() {
        mActions.logoClickProceed();
    }

    @Test
    public void testAutoProceedFromLogo_sameActivity() {
        mActions.logoWaitProceed();
    }
}

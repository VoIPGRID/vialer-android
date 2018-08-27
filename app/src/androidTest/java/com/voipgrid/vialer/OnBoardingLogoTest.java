package com.voipgrid.vialer;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;

import com.voipgrid.vialer.onboarding.SetupActivity;

public class OnBoardingLogoTest extends ActivityInstrumentationTestCase2<SetupActivity> {

    private TestActions mActions;

    public OnBoardingLogoTest() {
        super(SetupActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActions = new TestActions();
    }

    public void testClickProceedFromLogo_sameActivity() {
        mActions.logoClickProceed();
    }

    public void testAutoProceedFromLogo_sameActivity() {
        mActions.logoWaitProceed();
    }
}

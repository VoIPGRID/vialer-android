package com.voipgrid.vialer.test;

import com.voipgrid.vialer.BuildConfig;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * Abstract test class for tests that require Robolectric.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public abstract class RobolectricAbstractTest {
}

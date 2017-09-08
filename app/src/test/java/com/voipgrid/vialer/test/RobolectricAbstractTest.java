package com.voipgrid.vialer.test;

import com.voipgrid.vialer.BuildConfig;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Abstract test class for tests that require Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public abstract class RobolectricAbstractTest {
}

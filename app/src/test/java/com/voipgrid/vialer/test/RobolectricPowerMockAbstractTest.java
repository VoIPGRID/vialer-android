package com.voipgrid.vialer.test;

import org.junit.Rule;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;

/**
 * Abstract test class for tests that require Robolectric and PowerMock.
 */
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "retrofit2.*",
        "com.squareup.*", "com.google.*", "okio.*", "okhttp3.*" })
public abstract class RobolectricPowerMockAbstractTest extends RobolectricAbstractTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
}

package com.voipgrid.vialer.test;

import org.powermock.core.classloader.annotations.PowerMockIgnore;

/**
 * Abstract test class for tests that require Robolectric and PowerMock.
 */
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "retrofit2.*",
        "com.squareup.*", "com.google.*", "okio.*", "okhttp3.*" })
public abstract class RobolectricPowerMockAbstractTest extends RobolectricAbstractTest {
}

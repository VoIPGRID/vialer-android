package com.voipgrid.vialer.test;

import org.powermock.core.classloader.annotations.PowerMockIgnore;

/**
 * Abstract test class for tests that require Robolectric and PowerMock.
 */
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
public abstract class RobolectricPowerMockAbstractTest extends RobolectricAbstractTest {
}

package com.voipgrid.vialer.test.logging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.voipgrid.vialer.logging.DeviceInformation;
import com.voipgrid.vialer.logging.LogComposer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogComposerTest {

    @Mock DeviceInformation deviceInformation;

    private String dummyIdentifier = "dummyIdentifier";
    private String dummyAppVersion = "1.0.0";
    private LogComposer classUnderTest;

    @Before
    public void setUp() throws Exception {
        when(deviceInformation.getDeviceName()).thenReturn("dummydevice");
        when(deviceInformation.getConnectionType()).thenReturn("dummyconnectiontype");
        classUnderTest = new LogComposer(deviceInformation, dummyIdentifier, dummyAppVersion);
    }

    @Test
    public void it_composes_a_log_message() {
        String message = classUnderTest.compose("DEBUG", "TEST TAG", "TEST MESSAGE");
        assertEquals("DEBUG dummyIdentifier - 1.0.0 - dummydevice - dummyconnectiontype - TEST TAG - TEST MESSAGE", message);
    }
}

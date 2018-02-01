package com.voipgrid.vialer.test.logging;

import android.content.Context;

import com.logentries.logger.AsyncLoggingWorker;
import com.voipgrid.vialer.logging.LogEntriesFactory;
import com.voipgrid.vialer.logging.VialerLogger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VialerLoggerTest {

    @Mock Context context;

    @Mock LogEntriesFactory logEntriesFactory;

    @Mock AsyncLoggingWorker loggingWorker1, loggingWorker2, loggingWorker3;

    private VialerLogger classUnderTest;

    @Before
    public void setUp() throws Exception {
        classUnderTest = new VialerLogger(context, logEntriesFactory);
    }

    @Test
    public void it_sends_the_log_message_to_all_configured_environments() {
        when(logEntriesFactory.createLogger(anyString(), anyObject())).thenReturn(loggingWorker1, loggingWorker2, loggingWorker3);

        classUnderTest.initialize(new String[] {"token1", "token2", "token3"});

        String logMessage = "Hello, world!";

        classUnderTest.log(logMessage);
        verify(loggingWorker1).addLineToQueue(logMessage);
        verify(loggingWorker2).addLineToQueue(logMessage);
        verify(loggingWorker3).addLineToQueue(logMessage);
    }

    @Test
    public void it_does_not_attempt_to_create_environments_with_null_or_empty_tokens() {
        classUnderTest.initialize(new String[] {"token1", null, ""});

        verify(logEntriesFactory).createLogger("token1", context);
        verify(logEntriesFactory, never()).createLogger(null, context);
        verify(logEntriesFactory, never()).createLogger("", context);
    }

    @Test
    public void it_handles_the_factory_not_being_able_to_create_an_environment() {
        when(logEntriesFactory.createLogger("invalidToken", context)).thenReturn(null);
        classUnderTest.initialize(new String[] {"invalidToken"});
        classUnderTest.log("A log message");
    }
}
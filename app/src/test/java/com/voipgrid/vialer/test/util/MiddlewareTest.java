package com.voipgrid.vialer.test.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.test.RobolectricPowerMockAbstractTest;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.Middleware;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;

import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for the Middleware class.
 */
@PrepareForTest(Middleware.class)
public class MiddlewareTest extends RobolectricPowerMockAbstractTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    /**
     * Setup dependencies for tests.
     */
    @Before
    public void setUp() {
        JsonStorage jsonStorage = new JsonStorage(RuntimeEnvironment.application);
        SystemUser systemUser = new SystemUser();
        PhoneAccount phoneAccount = new PhoneAccount();

        // Systemuser with username and password.
        systemUser.setFirstName("VoIP");
        systemUser.setLastName("GRID");
        systemUser.setEmail("info@example.com");
        systemUser.setPassword("password");

        // Phone account for registration.
        phoneAccount.setAccountId("123456789");

        jsonStorage.save(systemUser);
        jsonStorage.save(phoneAccount);

        // Make sure all requirements are met for SIP.
        Preferences prefs = new Preferences(RuntimeEnvironment.application);
        prefs.setSipPermission(true);
        prefs.setSipEnabled(true);
    }

    /**
     * Test registering at the middleware.
     * @throws Exception
     */
    @Ignore("Robolectric SNAPSHOT breaks PowerMock functionality, " +
            "see https://github.com/robolectric/robolectric/issues/2208")
    @Test
    public void middlewareRegisterTest() throws Exception {
        // Create fake web server.
        MockWebServer server = new MockWebServer();
        // Make sure the server gives a 200 response the first time.
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        // Start the web server.
        server.start();

        // Get the URL to talk to the fake server.
        URL fakeServerUrl = server.getUrl("");

        Context context = RuntimeEnvironment.application;
        String fakePushToken = "APA91bHPRgkF3JUikC4ENAHEeMrd41Zxv3hVZjC9KtT8OvPVGJ-hQMRKR" +
                "rZuJAEcl7B338qju59zJMjw2DELjzEvxwYv7hH5Ynpc1ODQ0aT4U4OFEeco8ohsN5PjL1iC2" +
                "dNtk2BAokeMCg2ZXKqpc8FXKmhX94kIxQ";

        // Mock the method that returns the api base URL to return the fake server URL.
        PowerMockito.mockStatic(Middleware.class);
        when(Middleware.getBaseApiUrl(Matchers.any(Context.class)))
                .thenReturn(fakeServerUrl.toString());

        // Register call.
        Middleware.register(context, fakePushToken);

        // Check the if the request that was made meets requirements in the middleware
        // api docs.
        RecordedRequest request = server.takeRequest();

        // TODO After Robolectic issue is fixed: Check request that was made.

        // Check registration status.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int status = preferences.getInt("VIALER_REGISTRATION_STATUS", 10);

        assertTrue(status == 1);

        // Shutdown the fake web server.
        server.shutdown();
    }

    /**
     * Test registering at the middleware.
     * @throws Exception
     */
    @Ignore("Robolectric SNAPSHOT breaks PowerMock functionality, " +
            "see https://github.com/robolectric/robolectric/issues/2208")
    @Test
    public void middlewareUnregisterTest() throws Exception {
        // Create fake web server.
        MockWebServer server = new MockWebServer();
        // Make sure the server gives a 200 response the first time.
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        // Start the web server.
        server.start();

        // Get the URL to talk to the fake server.
        URL fakeServerUrl = server.getUrl("");

        Context context = RuntimeEnvironment.application;

        // Mock the method that returns the api base URL to return the fake server URL.
        PowerMockito.mockStatic(Middleware.class);
        when(Middleware.getBaseApiUrl(Matchers.any(Context.class)))
                .thenReturn(fakeServerUrl.toString());

        // Register call.
        Middleware.unregister(context);

        // Check the if the request that was made meets requirements in the middleware
        // api docs.
        RecordedRequest request = server.takeRequest();

        // TODO After Robolectic issue is fixed: Check request that was made.

        // Check registration status.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int status = preferences.getInt("VIALER_REGISTRATION_STATUS", 10);

        assertTrue(status == -1);

        // Shutdown the fake web server.
        server.shutdown();
    }

}

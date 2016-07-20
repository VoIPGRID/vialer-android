package com.voipgrid.vialer.test.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.test.RobolectricPowerMockAbstractTest;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.MiddlewareHelper;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Tests for the MiddlewareHelper class.
 */
@PrepareForTest(MiddlewareHelper.class)
public class MiddlewareHelperTest extends RobolectricPowerMockAbstractTest {

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
    @Test
    public void middlewareRegisterTest() throws Exception {
        // Create fake web server.
        MockWebServer server = new MockWebServer();
        // Make sure the server gives a 200 response the first time.
        server.enqueue(new MockResponse().setResponseCode(200));

        // Start the web server.
        server.start();

        // Get the URL to talk to the fake server.
        HttpUrl fakeServerUrl = server.url("");

        Context context = RuntimeEnvironment.application;
        String fakePushToken = "APA91bHPRgkF3JUikC4ENAHEeMrd41Zxv3hVZjC9KtT8OvPVGJ-hQMRKR" +
                "rZuJAEcl7B338qju59zJMjw2DELjzEvxwYv7hH5Ynpc1ODQ0aT4U4OFEeco8ohsN5PjL1iC2" +
                "dNtk2BAokeMCg2ZXKqpc8FXKmhX94kIxQ";

        // Mock the method that returns the api base URL to return the fake server URL.
        spy(MiddlewareHelper.class);
        stub(PowerMockito.method(MiddlewareHelper.class, "getBaseApiUrl", Context.class))
                .toReturn(fakeServerUrl.toString());

        // Make sure the mocked method returns the right value.
        assertTrue(MiddlewareHelper.getBaseApiUrl(context).equals(fakeServerUrl.toString()));

        // Register call.
        MiddlewareHelper.register(context, fakePushToken);

        // Check the if the request that was made meets requirements in the middleware
        // api docs.
        RecordedRequest request = server.takeRequest();

        // Check if auth header is set.
        assertNotNull(request.getHeader("Authorization"));

        String body = request.getBody().readUtf8();

        // Make sure required values are present.
        assertTrue(body.contains("sip_user_id"));
        assertTrue(body.contains("token"));
        assertTrue(body.contains("app"));

        // Check registration status.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int status = preferences.getInt(MiddlewareHelper.Constants.REGISTRATION_STATUS, 10);

        assertEquals(MiddlewareHelper.Constants.STATUS_REGISTERED, status);

        // Shutdown the fake web server.
        server.shutdown();
    }

    /**
     * Test registering at the middleware.
     * @throws Exception
     */
    @Test
    public void middlewareUnregisterTest() throws Exception {
        // Create fake web server.
        MockWebServer server = new MockWebServer();
        // Make sure the server gives a 200 response the first time.
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        // Start the web server.
        server.start();

        // Get the URL to talk to the fake server.
        HttpUrl fakeServerUrl = server.url("");

        Context context = RuntimeEnvironment.application;

        // Mock the method that returns the api base URL to return the fake server URL.
        spy(MiddlewareHelper.class);
        stub(PowerMockito.method(MiddlewareHelper.class, "getBaseApiUrl", Context.class))
                .toReturn(fakeServerUrl.toString());

        // Make sure the mocked method returns the right value.
        assertTrue(MiddlewareHelper.getBaseApiUrl(context).equals(fakeServerUrl.toString()));

        // Unregister call.
        MiddlewareHelper.unregister(context);

        // Check the if the request that was made meets requirements in the middleware
        // api docs.
        RecordedRequest request = server.takeRequest();

        assertNotNull(request.getHeader("Authorization"));

        String body = request.getBody().readUtf8();

        // Make sure required values are present.
        assertTrue(body.contains("sip_user_id"));
        assertTrue(body.contains("token"));
        assertTrue(body.contains("app"));

        // Check registration status.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int status = preferences.getInt(MiddlewareHelper.Constants.REGISTRATION_STATUS, 10);

        assertTrue(status == MiddlewareHelper.Constants.STATUS_UNREGISTERED);

        // Shutdown the fake web server.
        server.shutdown();
    }

}

package com.voipgrid.vialer.test.util;

import android.content.Context;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.test.RobolectricPowerMockAbstractTest;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.middleware.MiddlewareHelper;
import com.voipgrid.vialer.util.PhoneAccountHelper;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

/**
 * PhoneAccountHelperTest tests the function provided by the PhoneAccountHelper class.
 */
@PrepareForTest({ServiceGenerator.class, MiddlewareHelper.class})
public class PhoneAccountHelperTest extends RobolectricPowerMockAbstractTest {

    /**
     * Setup dependencies for tests.
     */
    @Before
    public void setUp() {
        JsonStorage jsonStorage = new JsonStorage(RuntimeEnvironment.application);
        SystemUser systemUser = new SystemUser();

        // Systemuser with username and password.
        systemUser.setEmail("info@example.com");
        systemUser.setPassword("password");

        jsonStorage.save(systemUser);

        // Make sure all requirements are met for SIP.
        Preferences prefs = new Preferences(RuntimeEnvironment.application);
        prefs.setSipPermission(true);
        prefs.setSipEnabled(true);
    }

    /**
     * Function to return a JSON string like the api.
     * @return
     */
    private String createSystemuserResponse() {
        return "{\"allow_app_account\": true, \"app_account\": " +
                "\"/api/phoneaccount/basic/phoneaccount/24/\", \"change_password\": false, " +
                "\"client\": \"/api/apprelation/client/11/\", \"description\": " +
                "\"Generated systemuser\", \"email\": \"rbeer@leannonwest.info\", " +
                "\"first_name\": \"Kaye\", \"groups\": [\"/api/permission/systemgroup/31/\"], " +
                "\"id\": 21, \"language\": \"nl\", \"last_name\": \"Greenholt\", " +
                "\"mobile_nr\": null, \"outgoing_cli\": \"+31508009000\", \"partner\": null, " +
                "\"preposition\": \"\", \"resource_uri\": \"/api/permission/systemuser/21/\", " +
                "\"session_expiry\": true, \"timezone\": null, \"token\": \"\"}";
    }

    /**
     * Function to return a JSON string like the api.
     * @return
     */
    private String createPhoneAccountResponse() {
        return "{\"account_id\": 129710001, " +
                "\"active\": true, " +
                "\"callerid_name\": \"Hilary Schroeder\", " +
                "\"country\": \"/api/relation/country/nl/\", " +
                "\"description\": \"Random phoneaccount 1\", " +
                "\"internal_number\": 200, " +
                "\"password\": \"NjhtjhGHfgghyjhg\"}";
    }

    /**
     * Test the getLinkedPhoneAccount function on the PhoneAccountHelperClass.
     * @throws Exception
     */
    @Test
    public void getLinkedPhoneAccountTest() throws Exception {
        // Create fake web server.
        MockWebServer server = new MockWebServer();

        // This test will make 2 requests:
        // 1: To get the systemuser.
        // 2: To get the phone account linked on the systemuser.
        server.enqueue(new MockResponse().setResponseCode(200).setBody(createSystemuserResponse()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(createPhoneAccountResponse()));

        // Start the web server.
        server.start();

        // Get the URL to talk to the fake server.
        HttpUrl fakeServerUrl = server.url("");

        Context context = RuntimeEnvironment.application;

        // Mock methods for getting urls to to return fake api urls.
        spy(ServiceGenerator.class);
        stub(PowerMockito.method(ServiceGenerator.class, "getVgApiUrl", Context.class))
                .toReturn(fakeServerUrl.toString());

        // Make sure the mocked method returns the right value.
        assertTrue(ServiceGenerator.getVgApiUrl(context).equals(fakeServerUrl.toString()));

        // Do api calls to get linked phone account.
        PhoneAccountHelper helper = new PhoneAccountHelper(context);
        PhoneAccount phoneAccount = helper.getLinkedPhoneAccount();

        // Get both requests.
        RecordedRequest systemuserRequest = server.takeRequest();
        RecordedRequest phoneAccountRequest = server.takeRequest();

        // Make sure they have auth headers.
        assertNotNull(systemuserRequest.getHeader("Authorization"));
        assertNotNull(phoneAccountRequest.getHeader("Authorization"));

        // Check if the phone account is returned as object.
        assertTrue(phoneAccount.getAccountId().equals("129710001"));
        assertTrue(phoneAccount.getNumber().equals("200"));
        assertTrue(phoneAccount.getPassword().equals("NjhtjhGHfgghyjhg"));

        server.shutdown();
    }

}

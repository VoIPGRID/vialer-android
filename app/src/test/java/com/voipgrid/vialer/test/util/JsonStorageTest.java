package com.voipgrid.vialer.test.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.test.RobolectricAbstractTest;
import com.voipgrid.vialer.util.JsonStorage;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the JsonStorage class.
 */
public class JsonStorageTest extends RobolectricAbstractTest {

    /**
     * Test the JSON storage only clears JSON Classes from SharedPreferences.
     */
    @Test
    public void clearTest() {
        JsonStorage jsonStorage = new JsonStorage(RuntimeEnvironment.application);

        // Create class used in JSON storage.
        SystemUser systemUser = new SystemUser();
        systemUser.setMobileNumber("0508009000");
        systemUser.setOutgoingCli("0508009000");
        jsonStorage.save(systemUser);

        // Check if properly saved.
        assertTrue(jsonStorage.has(SystemUser.class));

        // Create setting not used in JSON storage.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(
                RuntimeEnvironment.application);
        pref.edit().putString("setting_key", "setting_value").apply();

        // Clear the JSON storage.
        jsonStorage.clear();

        // Check if SystemUser is gone.
        assertFalse(jsonStorage.has(SystemUser.class));
        // Check if random settings still exists.
        assertTrue(pref.contains("setting_key"));
    }
}

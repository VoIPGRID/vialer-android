package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles the storage of json objects.
 */
public class JsonStorage<T> {

    private SharedPreferences mPreferences;

    public JsonStorage(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void save(T type) {
        mPreferences
                .edit()
                .putString(type.getClass().getName(), new Gson().toJson(type))
                .apply();
    }

    public T get(Class<T> clss) {
        return new Gson().fromJson(
                mPreferences.getString(clss.getName(), null), clss);
    }

    public void remove(Class<T> clss) {
        if (has(clss)){
            mPreferences.edit().remove(clss.getName()).apply();
        }
    }

    public boolean has(Class<T> clss) {
        return mPreferences.contains(clss.getName());
    }

    /**
     * Function that generates a list of class names for clearing the SharedPreferences.
     * @return
     */
    private List<String> getClassesToClear() {
        List<String> classesToClear = new ArrayList<>();
        classesToClear.add(SystemUser.class.getName());
        classesToClear.add(PhoneAccount.class.getName());
        classesToClear.add(CallRecord[].class.getName());
        classesToClear.add(CallRecord.class.getName());
        return classesToClear;
    }

    /**
     * Clear only the json objects from the SharedPreferences.
     */
    public void clear() {
        SharedPreferences.Editor editor =  mPreferences.edit();

        List<String> classesToClear = getClassesToClear();

        for (int i = 0; i < classesToClear.size(); i++) {
            editor.remove(classesToClear.get(i));
        }

        // Make sure the registration is invalidated as well.
        editor.putInt(
                MiddlewareHelper.Constants.REGISTRATION_STATUS,
                MiddlewareHelper.Constants.STATUS_UPDATE_NEEDED
        );

        editor.apply();
    }
}

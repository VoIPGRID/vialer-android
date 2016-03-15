package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

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
     * Dangerous function! This clears all stored settings instead of only the things that
     * are put in with this class.
     */
    public void clear() {
        mPreferences.edit().clear().apply();
    }
}

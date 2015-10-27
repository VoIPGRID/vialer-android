package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

/**
 * Created by eltjo on 03/08/15.
 */
public class Storage<T> {

    private SharedPreferences mPreferences;


    public Storage(Context context) {
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

    public boolean has(Class<T> clss) {
        return mPreferences.contains(clss.getName());
    }

    public void clear() {
        mPreferences.edit().clear().apply();
    }
}

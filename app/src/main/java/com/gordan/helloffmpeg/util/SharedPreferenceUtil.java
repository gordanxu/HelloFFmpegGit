package com.gordan.helloffmpeg.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by gordan on 2018/4/23.
 */

public class SharedPreferenceUtil {
    final static String PREFERENCE_KEY = "sounderclient";

    public static boolean setString(Context mContext, String key, String value) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.edit().putString(key, value).commit();
    }

    public static String getString(Context mContext, String key) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.getString(key, "");
    }

    public static boolean setInt(Context mContext, String key, int value) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);

        return mSharedPreferences.edit().putInt(key, value).commit();
    }

    public static int getInt(Context mContext, String key) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.getInt(key, -1);
    }

    public static long getLong(Context mContext, String key) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.getLong(key, -1L);
    }

    public static boolean setLong(Context mContext, String key, long value) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.edit().putLong(key, value).commit();
    }

    public static boolean clearPreferenceKey(Context mContext, String key) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.edit().remove(key).commit();
    }

}

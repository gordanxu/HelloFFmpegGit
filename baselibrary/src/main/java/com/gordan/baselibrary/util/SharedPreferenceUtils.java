package com.gordan.baselibrary.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferenceUtils {
    final static String PREFERENCE_KEY = "BaseLibrary";

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

    public synchronized static boolean getBooleanValue(Context mContext, String key, boolean value) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY,Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, value);
    }

    public synchronized static boolean setBooleanValue(Context mContext, String key, boolean value) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.edit().putBoolean(key,value).commit();
    }

    public static boolean clearPreferenceKey(Context mContext, String key) {
        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        return mSharedPreferences.edit().remove(key).commit();
    }

    /***
     *
     *  默认偏好设置的删除 获取 设置等
     *
     * getSharedPreferences 与  getPreferences
     *
     * 参考：https://blog.csdn.net/w47_csdn/article/details/51766401
     *
     * @param context
     */
    public synchronized static void clear(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.clear();
        prefEditor.apply();
    }

}

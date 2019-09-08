package com.gordan.helloffmpeg;

import android.content.Intent;

import com.gordan.baselibrary.BaseApplication;
import com.gordan.baselibrary.util.LogUtils;

public class HelloApplication extends BaseApplication
{
    final static String TAG=HelloApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        LogUtils.i(TAG,"===onCreate===",false);

        //Intent intent=new Intent("com.gordan.helloffmpeg.MediaIntentService"); 启动报错
        Intent intent=new Intent(this,MediaIntentService.class);
        this.startService(intent);
    }
}

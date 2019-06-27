package com.gordan.helloffmpeg;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class BaseApplication extends Application implements Thread.UncaughtExceptionHandler
{
    final static String TAG=BaseApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"======onCreate()======");
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        Log.i(TAG,"======uncaughtException()======");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        Log.e(TAG,"content:"+sw.toString());
        //抓取日志打印并保存到txt文档中
        /*File sdcard = Environment.getExternalStorageDirectory();
        String logName = System.currentTimeMillis() + ".txt";
        Log.i(TAG, sdcard.getAbsolutePath() + "======log=====" + logName);
        saveLogInfoToFile(sdcard.getAbsolutePath(), logName, sw.toString());*/
    }

    private void saveLogInfoToFile(String path, String fileName, String content) {
        FileOutputStream fos = null;

        try {
            File file = new File(path, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(file);
            byte[] bytes = content.getBytes();
            fos.write(bytes);
            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

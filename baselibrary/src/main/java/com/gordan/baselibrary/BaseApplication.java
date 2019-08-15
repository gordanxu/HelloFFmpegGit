package com.gordan.baselibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.gordan.baselibrary.util.LogUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Stack;

public class BaseApplication extends Application implements Thread.UncaughtExceptionHandler {
    final static String TAG = BaseApplication.class.getSimpleName();

    private static BaseApplication mInstance;

    private Stack<Activity> mActivities = new Stack<Activity>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "====onCreate()======");
        mInstance=this;
        Thread.setDefaultUncaughtExceptionHandler(this);
        //初始化日志打印类
        LogUtils.init(this,true);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        Log.e(TAG, "======ErrorLog=====" + sw.toString());

        /*File sdcard = Environment.getExternalStorageDirectory();
        String logName = System.currentTimeMillis() + ".txt";
        Log.i(TAG, sdcard.getAbsolutePath() + "======log=====" + logName);
        saveLogInfoToFile(sdcard.getAbsolutePath(), logName, sw.toString());*/
    }

    /****
     *
     * 获取手机Android系统的相关信息
     *
     * @param softApp
     * @param ex
     * @return
     */
    private String toErrorLog(Application softApp, Throwable ex) {
        String info = null;
        ByteArrayOutputStream baos = null;
        PrintStream printStream = null;
        try {
            baos = new ByteArrayOutputStream();
            printStream = new PrintStream(baos);
            ex.printStackTrace(printStream);
            byte[] data = baos.toByteArray();
            info = new String(data);
            PackageManager manager = softApp.getPackageManager();
            PackageInfo pinfo = manager.getPackageInfo(softApp.getPackageName(), 0);
            info += ""+(char)(13)+(char)(10)+"Version:"+pinfo.versionName;
            info += ""+(char)(13)+(char)(10)+"VersionCode:"+pinfo.versionCode;
            info += ""+(char)(13)+(char)(10)+"Android:" + android.os.Build.MODEL + ","
                    + android.os.Build.VERSION.RELEASE;
            info += ""+(char)(13)+(char)(10)+"Hardware:"
                    + android.os.Build.BOARD + ","
                    + android.os.Build.BRAND + ","//手机品牌
                    + android.os.Build.CPU_ABI + ","
                    + android.os.Build.DEVICE + ","
                    + android.os.Build.DISPLAY + ","
                    + android.os.Build.FINGERPRINT + ","
                    + android.os.Build.HOST + ","
                    + android.os.Build.ID + ","
                    + android.os.Build.MANUFACTURER + ","
                    + android.os.Build.MODEL + ","//手机型号
                    + android.os.Build.PRODUCT + ","
                    + android.os.Build.TAGS + ","
                    + android.os.Build.TIME + ","
                    + android.os.Build.TYPE + ","
                    + android.os.Build.USER;
            data = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (printStream != null) {
                    printStream.close();
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return info;
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

    public void pushActivity(Activity activity) {
        mActivities.push(activity);
    }

    public boolean removeActivity(Activity activity)
    {
        return mActivities.remove(activity);
    }

    /***获取栈顶的元素之后还要移除***/
    public Activity popActivity()
    {
        return mActivities.pop();
    }

    /***单纯获取栈顶的元素***/
    public Activity peekActivity()
    {
        return mActivities.peek();
    }

    public void finishAllExceptOne(Class<?> clazz) {
        for (Activity activity : mActivities) {
            if (activity.getClass().equals(clazz)) {
                continue;
            }
            activity.finish();
        }
    }

    public void finishAll() {
        for (Activity activity : mActivities) {
            activity.finish();
        }
    }

    public static BaseApplication getInstance() {
        return mInstance;
    }
}

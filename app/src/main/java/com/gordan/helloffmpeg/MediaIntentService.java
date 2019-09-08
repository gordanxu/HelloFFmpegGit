package com.gordan.helloffmpeg;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import com.gordan.baselibrary.util.AssetsUtils;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.helloffmpeg.util.Constant;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MediaIntentService extends IntentService {

    final static String TAG = MediaIntentService.class.getSimpleName();

    SimpleDateFormat sdf;

    File sdcard;

    public MediaIntentService() {
        super("MediaIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        //这里就有一个矛盾的地方 如果用户第一次安装呢？第一次是没有写入SD卡权限的

        sdf=new SimpleDateFormat("HH:mm:ss");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //在主线程中

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogUtils.i(TAG,"===onHandleIntent===",false);
        //在工作线程中

        //检查是否有存储卡读写权限 如果没有读写权限将报空指针
        if(checkPermission())
        {
            sdcard = Environment.getExternalStorageDirectory();
            File cache = new File(sdcard, Constant.CACHE_FILE);
            if (!cache.exists()) {
                LogUtils.i(TAG, "===cache===" + cache.mkdir(), false);
            }
            //拷贝默认的背景音乐和水印图片
            AssetsUtils.copyAssetsToSDCard(this, "music", Constant.CACHE_FILE);
            AssetsUtils.copyAssetsToSDCard(this, "picture", Constant.CACHE_FILE);

            LogUtils.i(TAG,"start:===="+sdf.format(new Date(System.currentTimeMillis())),false);
            scanFile(sdcard);
            LogUtils.i(TAG,"end:===="+sdf.format(new Date(System.currentTimeMillis())),false);
        }
        else
        {
            LogUtils.i(TAG,"===has no permission===",false);
        }
    }

    public void scanFile(File file) {
        if (file != null && file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file2 : files) {
                if (file2.listFiles() == null) {

                    String fullName = file2.getPath();
                    LogUtils.i(TAG, "====scan===" + fullName, false);
                    if (fullName.endsWith(".mp4") || fullName.endsWith(".mp3")) {
                        sendBroadcastMedia(fullName);
                    }

                } else
                    scanFile(file2);
            }
        } else {
            LogUtils.i(TAG, "===file do not exists====", false);
        }
    }


    private void sendBroadcastMedia(String path) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path)));
        this.sendBroadcast(intent);
    }

    private boolean checkPermission()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
        {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
            {
                return true;
            }
        }

        return  false;
    }

}

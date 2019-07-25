package com.gordan.helloffmpeg;


import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.util.FfmpegUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.OnClick;


/****
 * 存在的问题：
 * 1 ffmpeg的库只有arm-v7a的 其它平台的就不行
 *
 *
 *
 *
 *
 * ****/

public class MainActivity extends BaseActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    FfmpegUtil mFfmpegUtil;

    File sdcard=null;

    ExecutorService mExecutorService;

    @Override
    protected int inflateResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExecutorService=Executors.newFixedThreadPool(1);
        sdcard= Environment.getExternalStorageDirectory();
        mFfmpegUtil = new FfmpegUtil();
    }

    String jniStr = "";

    String msg="";

    @OnClick({R.id.btn_cpu, R.id.btn_protocol, R.id.btn_codec, R.id.btn_filter, R.id.btn_format,
            R.id.button, R.id.btn_configure,R.id.btn_gl,R.id.btn_other})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cpu:
                jniStr = mFfmpegUtil.cpuInfo();
                showText(jniStr);

                break;

            case R.id.btn_codec:

                jniStr = mFfmpegUtil.avcodecinfo();
                //showToast(jniStr);
                Intent intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content",jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_format:
                jniStr = mFfmpegUtil.avformatinfo();
                //showToast(jniStr);

                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content",jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_protocol:
                jniStr = mFfmpegUtil.urlprotocolinfo();
                //showToast(jniStr);
                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content",jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_filter:
                jniStr = mFfmpegUtil.avfilterinfo();
                //showToast(jniStr);
                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content",jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_configure:
                jniStr = mFfmpegUtil.configurationinfo();

                showText(jniStr);
                break;

            case R.id.button:

                intent = new Intent(this, DecodeActivity.class);
                this.startActivity(intent);
                break;


            case R.id.btn_gl:

                intent = new Intent(this, OpenGLActivity.class);
                this.startActivity(intent);
                break;

            case R.id.btn_other:

                /*String url=sdcard.getAbsolutePath()+File.separator+"gordan.mp4";

                intent = new Intent(this,PlayerActivity.class);
                intent.putExtra("url",url);
                this.startActivity(intent);*/

                /*jniStr="ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"gordan.mp4 -i " +sdcard.getAbsolutePath()+File.separator+
                        "moive.mp3 -c:v copy -c:a mp3 -strict experimental -map 0:v:0 -map 1:a:0 " +
                        sdcard.getAbsolutePath()+File.separator+"output.mp4";*/

                //jniStr="ffmpeg -y -i "+sdcard.getAbsolutePath()+File.separator+"trip.mp3 -vn -acodec copy -ss 00:00:00 -t 00:01:32 "+sdcard.getAbsolutePath()+File.separator+"190725.mp3";

                //jniStr="ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"different.mp3 -acodec libmp3lame -q:a 8 "+sdcard.getAbsolutePath()+File.separator+"0725.mp3";

                //jniStr="ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"song.wav -acodec libmp3lame "+sdcard.getAbsolutePath()+File.separator+"output.mp3";

                //jniStr="./ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"gordan.mp4 -i "+sdcard.getAbsolutePath()+File.separator+"01.m4a -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 "+sdcard.getAbsolutePath()+File.separator+"output.mp4";

                jniStr="ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"gordan.mp4 -i "+sdcard.getAbsolutePath()+File.separator+"moive.mp3 -c:v copy -c:a mp3 -acodec libmp3lame -strict experimental -map 0:v:0 -map 1:a:0 "+sdcard.getAbsolutePath()+File.separator+"output.mp4";

                Log.i(TAG,"=====jniStr====="+jniStr);

                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        String[] cmd=jniStr.split(" ");
                        try {
                            int result=mFfmpegUtil.convertVideoFormat(cmd);
                            Log.i(TAG,"=====result====="+result);
                            msg="convert success!!!";
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            msg=e.getMessage();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                showText(msg);
                            }
                        });
                    }
                });
                break;
        }

    }




}

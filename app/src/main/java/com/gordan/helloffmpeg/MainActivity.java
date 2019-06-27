package com.gordan.helloffmpeg;

import android.Manifest;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.gordan.helloffmpeg.util.FfmpegUtil;
import com.gordan.helloffmpeg.util.FileUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.ButterKnife;
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

public class MainActivity extends AppCompatActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    FfmpegUtil mFfmpegUtil;

    File sdcard=null;

    ExecutorService mExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO},10000);

        mExecutorService=Executors.newFixedThreadPool(1);

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {

                FileUtil.copyFileFromAssets(MainActivity.this,"music",sdcard.getAbsolutePath());

            }
        });


        sdcard= Environment.getExternalStorageDirectory();
        mFfmpegUtil = new FfmpegUtil();
    }

    String jniStr = "";

    @OnClick({R.id.btn_cpu, R.id.btn_protocol, R.id.btn_codec, R.id.btn_filter, R.id.btn_format,
            R.id.button, R.id.btn_configure, R.id.btn_audio, R.id.btn_gl,R.id.btn_camera,R.id.btn_other})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cpu:
                jniStr = mFfmpegUtil.cpuInfo();
                showToast(jniStr);
                break;

            case R.id.btn_codec:
                jniStr = mFfmpegUtil.avcodecinfo();
                showToast(jniStr);
                break;

            case R.id.btn_format:
                jniStr = mFfmpegUtil.avformatinfo();
                showToast(jniStr);
                break;

            case R.id.btn_protocol:
                jniStr = mFfmpegUtil.urlprotocolinfo();
                showToast(jniStr);
                break;

            case R.id.btn_filter:
                jniStr = mFfmpegUtil.avfilterinfo();
                showToast(jniStr);
                break;

            case R.id.btn_configure:
                jniStr = mFfmpegUtil.configurationinfo();
                showToast(jniStr);
                break;

            case R.id.button:

                Intent intent = new Intent(this, DecodeActivity.class);
                this.startActivity(intent);
                break;

            case R.id.btn_audio:
                intent = new Intent(this, AudioActivity.class);
                this.startActivity(intent);
                break;


            case R.id.btn_gl:

                intent = new Intent(this, OpenGLActivity.class);
                this.startActivity(intent);
                break;

            case R.id.btn_camera:
                intent = new Intent(this,CameraActivity.class);
                this.startActivity(intent);
                break;

            case R.id.btn_other:

                String url=sdcard.getAbsolutePath()+File.separator+"gordan.mp4";

                intent = new Intent(this,PlayerActivity.class);
                intent.putExtra("url",url);
                this.startActivity(intent);

                /*jniStr="ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"gordan.mp4 -i " +sdcard.getAbsolutePath()+File.separator+
                        "ThereYouGo.mp3 -c:v copy -c:a mp3 -strict experimental -map 0:v:0 -map 1:a:0 " +
                        sdcard.getAbsolutePath()+File.separator+"output.mp4";

                Log.i(TAG,"=====jniStr====="+jniStr);

                new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        String[] cmd=jniStr.split(" ");

                        mFfmpegUtil.convertVideoFormat(cmd);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                showToast("convert success!!!");
                            }
                        });
                    }
                }).start();*/



                break;
        }

    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }
}

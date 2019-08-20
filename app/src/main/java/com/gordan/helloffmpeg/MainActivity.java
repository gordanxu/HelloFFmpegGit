package com.gordan.helloffmpeg;

import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.baselibrary.util.MD5Utils;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.util.FfmpegUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.OnClick;


/****
 * 存在的问题：
 * 1 ffmpeg的库只有arm-v7a的 其它平台的就不行
 *
 * cat /proc/cpuinfo 指令就能查看手机的CPU架构信息
 * CPU architecture属性就是 7 表示armeabi-v7a ; 8 表示arm64-v8
 *
 *  CPU architecture: 8
 *
 *
 * ****/

public class MainActivity extends BaseActivity {

    final static String TAG = MainActivity.class.getSimpleName();

    FfmpegUtil mFfmpegUtil;

    @Bind(R.id.et_command)
    EditText etCommand;

    File sdcard = null;

    ExecutorService mExecutorService;

    @Override
    protected int inflateResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {
            case Constant
                    .MSG_COMMAND_EXECUTE_FINISHED:

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                showText("命令执行成功！");
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExecutorService = Executors.newFixedThreadPool(1);
        sdcard = Environment.getExternalStorageDirectory();
        mFfmpegUtil = new FfmpegUtil();



    }

    MaterialDialog mProgressDialog;

    String jniStr = "";

    @OnClick({R.id.btn_cpu, R.id.btn_protocol, R.id.btn_codec, R.id.btn_filter, R.id.btn_format,
            R.id.btn_configure, R.id.tv_command_finish})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cpu:

                jniStr = mFfmpegUtil.cpuInfo();
                showText(jniStr);

                break;

            case R.id.btn_codec:

                jniStr = mFfmpegUtil.avcodecinfo();
                Intent intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content", jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_format:
                jniStr = mFfmpegUtil.avformatinfo();
                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content", jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_protocol:
                jniStr = mFfmpegUtil.urlprotocolinfo();
                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content", jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_filter:
                jniStr = mFfmpegUtil.avfilterinfo();
                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content", jniStr);
                this.startActivity(intent);
                break;

            case R.id.btn_configure:

                jniStr = mFfmpegUtil.configurationinfo();
                intent = new Intent(this, InfoActivity.class);
                intent.putExtra("content", jniStr);
                this.startActivity(intent);
                break;


            case R.id.tv_command_finish:

                //jniStr="ffmpeg -i "+sdcard.getAbsolutePath()+File.separator+"gordan.mp4 -i "+sdcard.getAbsolutePath()+File.separator+"moive.mp3 -c:v copy -c:a mp3 -acodec libmp3lame -strict experimental -map 0:v:0 -map 1:a:0 "+sdcard.getAbsolutePath()+File.separator+"output.mp4";

                jniStr = etCommand.getText() + "";

                if (TextUtils.isEmpty(jniStr)) {
                    showText("执行的命令不能为空！");
                    break;
                }

                Log.i(TAG, "=====jniStr=====" + jniStr);
                MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(this);
                mProgressDialog = mBuilder.content("命令执行中,请稍后...").progress(true, 100, true)
                        .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
                mProgressDialog.show();

                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {

                        //ffmpeg -i /sdcard/gordan.mp4 -vf \"movie=/home/xpzhi/test/apple.jpg,scale=600:439[watermask];[in][watermask] overlay=10:10 [out]\" /sdcard/0726.mp4
                        String[] cmd = jniStr.split(" ");
                        int result = mFfmpegUtil.convertVideoFormat(cmd);
                        Log.i(TAG, "=====result=====" + result);
                        mHandler.sendEmptyMessage(Constant.MSG_COMMAND_EXECUTE_FINISHED);

                    }
                });
                break;
        }

    }


}

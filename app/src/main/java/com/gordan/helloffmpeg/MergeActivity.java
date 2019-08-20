package com.gordan.helloffmpeg;


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

public class MergeActivity extends BaseActivity {

    final static String TAG=MergeActivity.class.getSimpleName();

    FfmpegUtil ffmpegUtil;

    File sdcardFile;

    ExecutorService mExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sdcardFile= Environment.getExternalStorageDirectory();

        ffmpegUtil=new FfmpegUtil();

         mExecutorService=Executors.newFixedThreadPool(2);

    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_merge;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @OnClick({R.id.tv_command_finish})
    public void onViewClick(View view)
    {
        String video=sdcardFile.getAbsolutePath()+File.separator+"input.h264";
        String audio=sdcardFile.getAbsolutePath()+File.separator+"yinpin.aac";
        String output=sdcardFile.getAbsolutePath()+File.separator+"gordanxu.ts";

        Log.i(TAG,"====video====="+video);
        Log.i(TAG,"====audio====="+audio);
        Log.i(TAG,"====output====="+output);
        Log.i(TAG,"===onViewClick===");

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {

                ffmpegUtil.mergeVideoAndAudio(video,audio,output);

                Log.i(TAG,"===merge finished===");
            }
        });


    }
}

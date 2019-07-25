package com.gordan.helloffmpeg;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.util.FfmpegUtil;

import java.io.File;

import butterknife.Bind;
import butterknife.OnClick;

public class DecodeActivity extends BaseActivity {

    final static String TAG=DecodeActivity.class.getSimpleName();

    @Bind(R.id.et_input)
    EditText etInput;

    @Bind(R.id.et_output)
    EditText etOutput;

    FfmpegUtil mFfmpegUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE},20000);

        mFfmpegUtil=new FfmpegUtil();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_decode;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @OnClick({R.id.btn_decode})
    public void onViewClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_decode:
                File sdcardFile = Environment.getExternalStorageDirectory();

                Log.i(TAG, "======path======" + sdcardFile.getAbsolutePath());

                String input = sdcardFile.getAbsolutePath() + File.separator + etInput.getText();

                input=input.trim();

                String output = sdcardFile.getAbsolutePath() + File.separator + etOutput.getText();

                output=output.trim();

                //如果放到主线程中将会卡住 解码的过程应该放到子线程中
                mFfmpegUtil.decode(input,output);



                showText("解码完成！");

                break;
        }
    }


}

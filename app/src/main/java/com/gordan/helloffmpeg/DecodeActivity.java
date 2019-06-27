package com.gordan.helloffmpeg;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.gordan.helloffmpeg.util.FfmpegUtil;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DecodeActivity extends Activity {

    final static String TAG=DecodeActivity.class.getSimpleName();

    @Bind(R.id.et_input)
    EditText etInput;

    @Bind(R.id.et_output)
    EditText etOutput;

    FfmpegUtil mFfmpegUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);
        ButterKnife.bind(this);

        mFfmpegUtil=new FfmpegUtil();
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

                showToast("解码完成！");

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

package com.gordan.helloffmpeg;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.util.FfmpegUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.OnClick;

public class MergeActivity extends BaseActivity {

    final static String TAG = MergeActivity.class.getSimpleName();

    int category;

    @Bind(R.id.tv_tips)
    TextView tvTips;
    @Bind(R.id.ll_source_media)
    LinearLayout llSourceMedia;
    @Bind(R.id.tv_video)
    TextView tvVideo;
    @Bind(R.id.tv_audio)
    TextView tvAudio;
    @Bind(R.id.et_source_media)
    EditText etSourceMedia;
    @Bind(R.id.et_dest_media)
    EditText etDestMedia;
    @Bind(R.id.et_video)
    EditText etVideo;
    @Bind(R.id.et_audio)
    EditText etAudio;

    @Bind(R.id.ll_dest_media)
    LinearLayout llDestMedia;

    FfmpegUtil ffmpegUtil;

    File sdcardFile;

    ExecutorService mExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Intent intent = getIntent();
            //1 分离 2 合并  默认是分离
            category = intent.getIntExtra("category", 1);
        }
        LogUtils.i(TAG, "====category====" + category, false);

        if (category == 2) {
            //合并
            llSourceMedia.setVisibility(View.GONE);

            tvVideo.setText("源视频:");
            tvAudio.setText("源音频:");

            llDestMedia.setVisibility(View.VISIBLE);
        }

        sdcardFile = Environment.getExternalStorageDirectory();

        tvTips.setText("设备存储卡路径: " + sdcardFile.getAbsolutePath());

        ffmpegUtil = new FfmpegUtil();

        mExecutorService = Executors.newFixedThreadPool(2);

    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_merge;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {

            case Constant.MSG_COPY_FINISHED:
                showText("存储卡路径复制成功！");
                break;

            case Constant.MSG_COMMAND_EXECUTE_FINISHED:

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                if (category == 1) {
                    showText("分离完成！");
                } else {
                    showText("合并完成！");
                }

                break;
        }

    }

    MaterialDialog mProgressDialog;

    public void showDialog(String text) {
        MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(this);
        mProgressDialog = mBuilder.content(text).contentColor(ContextCompat.getColor(this, R.color.colorAccent))
                .progress(true, 100, true)
                .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
        mProgressDialog.show();
    }

    String input = "", video = "", audio = "";

    @OnClick({R.id.tv_copy, R.id.tv_command_finish})
    public void onViewClick(View view) {
        LogUtils.i(TAG, "===onViewClick===", false);

        switch (view.getId()) {
            case R.id.tv_copy:
                String path = sdcardFile.getAbsolutePath();
                ClipboardManager mClipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
                mClipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
                    @Override
                    public void onPrimaryClipChanged() {

                        LogUtils.i(TAG, "===onPrimaryClipChanged====", false);

                        mHandler.sendEmptyMessage(Constant.MSG_COPY_FINISHED);

                    }
                });

                ClipData mClipData = ClipData.newPlainText("gordan", path);
                mClipboardManager.setPrimaryClip(mClipData);

                break;

            case R.id.tv_command_finish:

                if (category == 1) {
                    //分离
                    input = etSourceMedia.getText() + "";
                } else {
                    input = etDestMedia.getText() + "";
                }
                if(TextUtils.isEmpty(input))
                {
                    showText("媒体文件路径不能为空！");
                    break;
                }

                video = etVideo.getText() + "";
                if(TextUtils.isEmpty(video))
                {
                    showText("视频文件路径不能为空！");
                    break;
                }

                audio = etAudio.getText() + "";
                if(TextUtils.isEmpty(audio))
                {
                    showText("音频文件路径不能为空！");
                    break;
                }

                if (category == 1) {
                    //分离
                    showDialog("音视频分离中,请稍后...");
                } else {
                    showDialog("因视频合并中,请稍后...");
                }

                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {

                        if (category == 1) {
                            //分离
                            ffmpegUtil.separateVideoAndAudio(input, video, audio);
                        } else {
                            //合并
                            ffmpegUtil.mergeVideoAndAudio(video, audio, input);
                        }
                        LogUtils.i(TAG, "===finished===", false);
                        mHandler.sendEmptyMessage(Constant.MSG_COMMAND_EXECUTE_FINISHED);
                    }
                });
                break;
        }


    }
}

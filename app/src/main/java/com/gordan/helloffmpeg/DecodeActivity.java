package com.gordan.helloffmpeg;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.util.FfmpegUtil;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.OnClick;


/********
 * 将视频转换为   YUV 裸视频流
 *
 * 其实质也是利用了ffmpeg命令
 *
 *
 */
public class DecodeActivity extends BaseActivity {

    final static String TAG = DecodeActivity.class.getSimpleName();

    ExecutorService mExecutorService;

    FfmpegUtil mFfmpegUtil;

    @Bind(R.id.ll_video_output)
    LinearLayout llOutput;
    @Bind(R.id.iv_video_cover)
    ImageView ivCover;
    @Bind(R.id.tv_video_title)
    TextView tvTitle;
    @Bind(R.id.tv_video_output)
    TextView tvOutput;
    @Bind(R.id.tv_command_finish)
    TextView tvFinish;

    File sdcardFile;

    String input,output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFfmpegUtil = new FfmpegUtil();

        mExecutorService = Executors.newFixedThreadPool(2);

        sdcardFile = Environment.getExternalStorageDirectory();

        Log.i(TAG, "======path======" + sdcardFile.getAbsolutePath());
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_decode;
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

                llOutput.setVisibility(View.VISIBLE);
                tvOutput.setText(output);

                tvFinish.setEnabled(false);
                tvFinish.setClickable(false);
                tvFinish.setBackground(ContextCompat.getDrawable(this,R.drawable.button_bg_gray));

                showText("抽取完成！");

                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constant.FLAG_SCAN_VIDEO_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                Cursor cursor = cr.query(uri, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {

                        input = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(imagePath, MediaStore.Video.Thumbnails.MINI_KIND);

                        ivCover.setImageBitmap(bitmap);
                        tvTitle.setText(input);

                        tvFinish.setEnabled(true);
                        tvFinish.setClickable(true);
                        tvFinish.setBackground(ContextCompat.getDrawable(this,R.drawable.button_bg_red));


                    }
                }
            }
        }

    }

    MaterialDialog mProgressDialog;

    @OnClick({R.id.tv_choose,R.id.tv_command_finish})
    public void onViewClick(View view) {
        switch (view.getId()) {

            case R.id.tv_choose:
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, Constant.FLAG_SCAN_VIDEO_REQUEST_CODE);
                break;

            case R.id.tv_command_finish:


                MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(this);
                mProgressDialog = mBuilder.content("处理中,请稍后...").progress(true, 100, true)
                        .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
                mProgressDialog.show();


                Calendar mCalendar = Calendar.getInstance();

                String dateStr = mCalendar.get(Calendar.YEAR) + "" + (mCalendar.get(Calendar.MONTH) + 1) + "" + mCalendar.get(Calendar.DAY_OF_MONTH);

                Log.i(TAG, "=====dateStr====" + dateStr);

                output = sdcardFile.getAbsolutePath() + File.separator + dateStr+".yuv";

                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        mFfmpegUtil.decode(input, output);

                        mHandler.sendEmptyMessage(Constant.MSG_COMMAND_EXECUTE_FINISHED);
                    }
                });


                break;
        }
    }


}

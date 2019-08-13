package com.gordan.helloffmpeg;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
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

public class MarkActivity extends BaseActivity {


    final static String TAG = MarkActivity.class.getSimpleName();

    DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    @Bind(R.id.ll_video_output)
    LinearLayout llVideoOutput;

    @Bind(R.id.iv_video_cover)
    ImageView ivCover;

    @Bind(R.id.tv_video_title)
    TextView tvTitle;

    @Bind(R.id.tv_choose)
    TextView tvChoose;

    @Bind(R.id.tv_video_output)
    TextView tvOutputPath;

    @Bind(R.id.tv_command_finish)
    TextView tvFinish;

    String input, output;

    FfmpegUtil mFfmpegUtil;

    ExecutorService mExecutorService = null;

    Calendar mCalendar;

    File sdFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "=======onCreate()======");

        this.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

        sdFile = Environment.getExternalStorageDirectory();

        mExecutorService = Executors.newFixedThreadPool(1);

        mFfmpegUtil = new FfmpegUtil();


    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_mark;
    }

    @Override
    protected void handleBaseMessage(Message message) {
        Log.i(TAG, "=======handleBaseMessage()======");
        switch (message.what) {
            case Constant.MSG_COMMAND_EXECUTE_FINISHED:


                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                showText("处理完成！");

                llVideoOutput.setVisibility(View.VISIBLE);
                tvOutputPath.setText(output);


                tvFinish.setEnabled(false);
                tvFinish.setClickable(false);
                tvFinish.setBackground(ContextCompat.getDrawable(this, R.drawable.button_bg_gray));


                break;
        }
    }

    MaterialDialog mProgressDialog;

    @OnClick({R.id.tv_choose, R.id.tv_command_finish})
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

                mCalendar = Calendar.getInstance();

                String dateStr = mCalendar.get(Calendar.YEAR) + "" + (mCalendar.get(Calendar.MONTH) + 1) + "" + mCalendar.get(Calendar.DAY_OF_MONTH);

                Log.i(TAG, "=====dateStr====" + dateStr);
                //获取视频源文件的后缀名
                String fileName = input.substring(input.lastIndexOf("."));

                fileName = (dateStr + fileName);
                Log.i(TAG, "=====fileName====" + fileName);
                output = sdFile.getAbsolutePath() + File.separator + fileName;

                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {


                        String jniStr = "ffmpeg -i " + input + " -i " + sdFile.getAbsolutePath() +
                                "/video_water.png -filter_complex overlay=50:" + (mDisplayMetrics.heightPixels - 150) + " " + output;

                        Log.i(TAG, "=======jniStr======" + jniStr);

                        String[] cmd = jniStr.split(" ");

                        mFfmpegUtil.convertVideoFormat(cmd);

                        mHandler.sendEmptyMessage(Constant.MSG_COMMAND_EXECUTE_FINISHED);

                    }
                });


                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "=======onActivityResult()======");
        if (requestCode == Constant.FLAG_SCAN_VIDEO_REQUEST_CODE) {

            if (resultCode == RESULT_OK) {

                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                /** 数据库查询操作。
                 * 第一个参数 uri：为要查询的数据库+表的名称。
                 * 第二个参数 projection ： 要查询的列。
                 * 第三个参数 selection ： 查询的条件，相当于SQL where。
                 * 第三个参数 selectionArgs ： 查询条件的参数，相当于 ？。
                 * 第四个参数 sortOrder ： 结果排序。
                 */
                Cursor cursor = cr.query(uri, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        // 视频ID:MediaStore.Audio.Media._ID
                        int videoId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                        // 视频名称：MediaStore.Audio.Media.TITLE
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                        // 视频路径：MediaStore.Audio.Media.DATA
                        input = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        // 视频时长：MediaStore.Audio.Media.DURATION
                        int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                        // 视频大小：MediaStore.Audio.Media.SIZE
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                        Log.i(TAG, videoId + "======video======" + duration);
                        // 视频缩略图路径：MediaStore.Images.Media.DATA
                        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                        // 缩略图ID:MediaStore.Audio.Media._ID
                        int imageId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                        // 方法一 Thumbnails 利用createVideoThumbnail 通过路径得到缩略图，保持为视频的默认比例
                        // 第一个参数为 ContentResolver，第二个参数为视频缩略图ID， 第三个参数kind有两种为：MICRO_KIND和MINI_KIND 字面意思理解为微型和迷你两种缩略模式，前者分辨率更低一些。
                        //Bitmap bitmap1 = MediaStore.Video.Thumbnails.getThumbnail(cr, imageId, MediaStore.Video.Thumbnails.MICRO_KIND, null);


                        // 方法二 ThumbnailUtils 利用createVideoThumbnail 通过路径得到缩略图，保持为视频的默认比例
                        // 第一个参数为 视频/缩略图的位置，第二个依旧是分辨率相关的kind
                        Bitmap bitmap2 = ThumbnailUtils.createVideoThumbnail(imagePath, MediaStore.Video.Thumbnails.MINI_KIND);
                        // 如果追求更好的话可以利用 ThumbnailUtils.extractThumbnail 把缩略图转化为的制定大小
                        //ThumbnailUtils.extractThumbnail(bitmap2, 100,100 ,ThumbnailUtils.OPTIONS_RECYCLE_INPUT);


                        ivCover.setImageBitmap(bitmap2);
                        tvTitle.setText(input);


                        if (!tvFinish.isEnabled()) {
                            tvChoose.setText("重新选择");

                            tvFinish.setEnabled(true);
                            tvFinish.setClickable(true);
                            tvFinish.setBackground(ContextCompat.getDrawable(this, R.drawable.button_bg_red));
                        }


                    }
                    cursor.close();
                }
            }
        }


    }
}

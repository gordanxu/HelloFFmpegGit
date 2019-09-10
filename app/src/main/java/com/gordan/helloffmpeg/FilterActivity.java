package com.gordan.helloffmpeg;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.util.ImageUtils;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.baselibrary.util.ScreenUtils;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.view.CameraView;
import com.gordan.helloffmpeg.controller.SensorControler;

import java.io.File;

import butterknife.Bind;
import butterknife.OnClick;


/****
 * 拍照保存的功能 还是使用了原版的老方法 只是捕捉到了摄像头的画面滤镜的效果并没有保存到照片中
 *
 *
 *
 * 录像的时候最多只能录制 15 秒
 *
 *
 *
 * ***/
public class FilterActivity extends BaseActivity implements View.OnTouchListener,
        SensorControler.CameraFocusListener, Camera.PictureCallback {

    final static String TAG = FilterActivity.class.getSimpleName();

    @Bind(R.id.cv_container)
    CameraView mCameraView;

    int screenWidth, screenHeight;

    File sdcardFile;

    boolean recordFlag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screenWidth = ScreenUtils.getScreenW(this);
        screenHeight = ScreenUtils.getScreenH(this);

        mCameraView.setOnTouchListener(this);

        sdcardFile = Environment.getExternalStorageDirectory();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_filter;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {

            case Constant.MSG_TAKE_VIDEO_END:


                String savePath = message.obj + "";

                mCameraView.stopRecord();
                recordFlag = false;
                showText("拍摄完成，视频保存路径:" + savePath);

                break;

            case Constant.MSG_TAKE_PICTURE_FINISHED:

                showText("拍照完成，照片保存路径:" + message.obj);
                //通知Android系统保存拍摄的视频文件记录到MediaStore数据库中
                sendBroadcastMedia(message.obj+"");
                shootSound("file:///system/media/audio/ui/camera_click.ogg");
                mCameraView.onResume();

                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "=====onResume()=====");

        showText("触摸屏幕可以对焦~");

        mCameraView.onResume();
    }

    @OnClick({R.id.tv_filter_sun, R.id.tv_filter_cool, R.id.tv_filter_warm, R.id.iv_photo, R.id.iv_video})
    public void onViewClick(View view) {
        switch (view.getId()) {

            case R.id.iv_video:

                String fileName = System.currentTimeMillis() + ".mp4";
                String savePath = sdcardFile.getAbsolutePath() + File.separator + Constant.CACHE_FILE + File.separator + fileName;

                Message message = new Message();
                message.what = Constant.MSG_TAKE_VIDEO_END;
                message.obj = savePath;

                if (!recordFlag) {
                    showText("开始拍摄，再按一次结束拍摄！");
                    mCameraView.setSavePath(savePath);
                    mCameraView.startRecord();
                    recordFlag = true;

                    mHandler.sendMessageDelayed(message, Constant.VIDEO_TAKE_MAX_TIME);
                } else {
                    if (mHandler.hasMessages(Constant.MSG_TAKE_VIDEO_END)) {
                        mHandler.removeMessages(Constant.MSG_TAKE_VIDEO_END);
                    }
                    mHandler.sendMessage(message);

                }


                break;

            case R.id.iv_photo:

                mCameraView.takePicture(this);

                break;

            case R.id.tv_filter_sun:

                mCameraView.switchFilterIndex(0);

                break;

            case R.id.tv_filter_cool:
                mCameraView.switchFilterIndex(1);
                break;

            case R.id.tv_filter_warm:
                mCameraView.switchFilterIndex(2);
                break;
        }


    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        //触摸屏幕 相机自动聚焦

        switch (event.getAction()) {

            case MotionEvent.ACTION_UP:
                float sRawX = event.getRawX();
                float sRawY = event.getRawY();
                float rawY = sRawY * screenWidth / screenHeight;
                float temp = sRawX;
                float rawX = rawY;
                rawY = (screenWidth - temp) * screenHeight / screenWidth;

                Point point = new Point((int) rawX, (int) rawY);
                mCameraView.onFocus(point, screenWidth, screenHeight, null);
        }
        return true;
    }

    @Override
    public void onFocus() {

        Point point = new Point(screenWidth / 2, screenHeight / 2);
        mCameraView.onFocus(point, screenWidth, screenHeight, null);
    }

    private void sendBroadcastMedia(String path)
    {
        Intent intent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(new File(path)));
        this.sendBroadcast(intent);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "====take picture callback====");
        try {
            String path = sdcardFile.getAbsolutePath() + File.separator + Constant.CACHE_FILE +
                    File.separator + System.currentTimeMillis() + ".jpg";
            LogUtils.i(TAG, "==path==" + path, false);
            File imageFile = new File(path);
            if (!imageFile.exists()) {

                if (!imageFile.createNewFile()) {
                    Log.i(TAG, "====create file failed so return====");
                    return;
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {

                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                    Bitmap normalBitmap = ImageUtils.getRotateBitmap(bitmap, 90.0f);

                    ImageUtils.saveBitmap2File(normalBitmap, imageFile);

                    Message msg = new Message();
                    msg.what = Constant.MSG_TAKE_PICTURE_FINISHED;
                    msg.obj = path;
                    mHandler.sendMessage(msg);

                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "===onStop()====");
        mCameraView.onPause();
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "===onDestroy()====");
        mCameraView.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        super.onDestroy();
    }

    MediaPlayer mMediaPlayer = null;

    /***
     * 播放拍照的系统提示音
     * ***/
    private void shootSound(String url) {

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(this, Uri.parse(url));
            } else {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                mMediaPlayer.setDataSource(this, Uri.parse(url));
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {

                        mMediaPlayer.start();
                    }
                });
            }
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

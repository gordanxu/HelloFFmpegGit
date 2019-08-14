package com.gordan.helloffmpeg;

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
import com.gordan.baselibrary.util.ScreenUtils;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.view.CameraView;
import com.gordan.helloffmpeg.controller.SensorControler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;



/****
 * 拍照保存的功能 还是使用了原版的老方法 只是捕捉到了摄像头的画面滤镜的效果并没有保存到照片中
 *
 *
 * ***/
public class FilterActivity extends BaseActivity implements View.OnTouchListener,
        SensorControler.CameraFocusListener {

    final static String TAG = FilterActivity.class.getSimpleName();

    @Bind(R.id.cv_container)
    CameraView mCameraView;

    int screenWidth, screenHeight;

    File sdcardFile;

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
            case Constant.MSG_TAKE_PICTURE:
                shootSound("file:///system/media/audio/ui/camera_click.ogg");
                mCameraView.onResume();

                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @OnClick({R.id.tv_filter_sun, R.id.tv_filter_cool, R.id.tv_filter_warm, R.id.iv_photo})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.iv_photo:

                mCameraView.takePicture(new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                        Log.i(TAG, "====take picture callback====");

                        try {

                            File imageFile = new File(sdcardFile, "gordan_0814.jpg");
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

                                    saveBitmap(normalBitmap, imageFile);

                                    mHandler.sendEmptyMessage(Constant.MSG_TAKE_PICTURE);

                                }
                            }).start();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                });

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
                // mFocus.startFocus(new Point((int) sRawX, (int) sRawY));
        }


        return true;
    }

    @Override
    public void onFocus() {

        Point point = new Point(screenWidth / 2, screenHeight / 2);
        mCameraView.onFocus(point, screenWidth, screenHeight, null);

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

    public void saveBitmap(Bitmap b, File file) {

        try {
            FileOutputStream fout = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            fout.close();
            Log.i(TAG, "saveBitmap成功");
        } catch (IOException e) {
            Log.i(TAG, "saveBitmap:失败");
            e.printStackTrace();
        }

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

    private void saveImageByte2File(byte[] image, File file) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(image);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "=====save image success=====");
            mHandler.sendEmptyMessage(Constant.MSG_TAKE_PICTURE);
        }
    }

}

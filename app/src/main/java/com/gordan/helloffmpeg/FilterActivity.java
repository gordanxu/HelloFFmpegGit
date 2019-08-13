package com.gordan.helloffmpeg;


import android.graphics.Point;
import android.os.Bundle;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.util.ScreenUtils;
import com.gordan.helloffmpeg.view.CameraView;
import com.gordan.helloffmpeg.controller.SensorControler;

import butterknife.Bind;
import butterknife.OnClick;

public class FilterActivity extends BaseActivity implements View.OnTouchListener,
        SensorControler.CameraFocusListener {
    @Bind(R.id.cv_container)
    CameraView mCameraView;

    int screenWidth, screenHeight;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screenWidth = ScreenUtils.getScreenW(this);
        screenHeight = ScreenUtils.getScreenH(this);

        mCameraView.setOnTouchListener(this);
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_filter;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @OnClick({R.id.tv_filter_sun,R.id.tv_filter_cool,R.id.tv_filter_warm,R.id.iv_photo})
    public void onViewClick(View view)
    {
        switch (view.getId())
        {
            case R.id.iv_photo:break;

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

        mCameraView.onPause();
    }


    @Override
    protected void onDestroy() {
        mCameraView.onDestroy();
        super.onDestroy();
    }
}

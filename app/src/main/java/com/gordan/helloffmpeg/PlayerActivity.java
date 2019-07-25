package com.gordan.helloffmpeg;

import android.content.Intent;
import android.os.Message;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.view.EmptyControlVideo;

import butterknife.Bind;
import butterknife.OnClick;


public class PlayerActivity extends BaseActivity {

    final static String TAG = PlayerActivity.class.getSimpleName();

    String url = "http://vfx.mtime.cn/Video/2019/03/12/mp4/190312143927981075.mp4";

    boolean firstFlag;

    @Bind(R.id.ll_camera)
    LinearLayout llCamera;

    @Bind(R.id.ijk_player)
    EmptyControlVideo ijkPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firstFlag=true;
        Log.i(TAG, "======onCreate()======");
        if (getIntent() != null) {
            Intent intent = getIntent();
            String tempUrl = intent.getStringExtra("url");
            if (!TextUtils.isEmpty(tempUrl)) {
                url = tempUrl;
            }
        }
        Log.i(TAG, "====url=====" + url);
        //轮询播放（因为没找到播放结束的监听）
        ijkPlayer.setLooping(true);
        ijkPlayer.setUp(url, true, "我的抖音");
        ijkPlayer.startPlayLogic();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "======onResume()======");
        //初次进入不需要调用 resume()周期方法
        if(!firstFlag)
        {
            ijkPlayer.onVideoResume();
        }
        firstFlag=false;

    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_player;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @OnClick({R.id.ll_camera})
    public void onViewClick(View view) {
        Log.i(TAG, "=======onViewClick======");
        switch (view.getId()) {
            case R.id.ll_camera:

                Intent intent = new Intent(this, CameraActivity.class);
                this.startActivity(intent);


                break;
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "======onPause()======");
        ijkPlayer.onVideoPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "======onStop()======");

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "======onDestroy()======");
        //ijkPlayer.onVideoReset();
        ijkPlayer.release();
        super.onDestroy();
    }
}

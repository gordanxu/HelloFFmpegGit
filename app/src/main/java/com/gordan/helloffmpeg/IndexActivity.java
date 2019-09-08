package com.gordan.helloffmpeg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.gordan.baselibrary.BaseActivity;

import butterknife.OnClick;

public class IndexActivity extends BaseActivity {

    final static String TAG = IndexActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_index;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    Intent intent = null;

    @OnClick({R.id.ll_menu_video, R.id.ll_menu_music, R.id.ll_menu_merge, R.id.ll_menu_convert,
            R.id.ll_menu_audio, R.id.ll_menu_filter, R.id.ll_menu_separate, R.id.ll_menu_command,
            R.id.ll_menu_video_mine})
    public void onViewClick(View view) {

        //检查应用需要的权限
        if (!checkPermission()) {
            return;
        }

        switch (view.getId()) {

            case R.id.ll_menu_video_mine:

                intent = new Intent(this, VideoMine.class);
                this.startActivity(intent);

                break;
            case R.id.ll_menu_video:

                intent = new Intent(this, CameraActivity.class);
                this.startActivity(intent);

                break;

            case R.id.ll_menu_music:

                intent = new Intent(this, ListActivity.class);
                this.startActivity(intent);

                break;

            case R.id.ll_menu_merge:

                intent = new Intent(this, MergeActivity.class);
                intent.putExtra("category", 2);
                this.startActivity(intent);

                break;

            case R.id.ll_menu_convert:
                intent = new Intent(this, MarkActivity.class);
                this.startActivity(intent);
                break;

            case R.id.ll_menu_audio:

                intent = new Intent(this, AudioActivity.class);
                this.startActivity(intent);
                break;


            case R.id.ll_menu_filter:

                intent = new Intent(this, FilterActivity.class);
                this.startActivity(intent);

                break;


            case R.id.ll_menu_separate:

                intent = new Intent(this, MergeActivity.class);
                intent.putExtra("category", 1);
                this.startActivity(intent);

                break;

            case R.id.ll_menu_command:

                intent = new Intent(this,MainActivity.class);
                this.startActivity(intent);

                break;
        }
    }


    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermission();
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermission();
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermission();
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            requestPermission();
            return false;
        }

        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO}, 10000);
    }
}

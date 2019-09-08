package com.gordan.helloffmpeg;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.baselibrary.view.LinearLayoutColorDivider;
import com.gordan.helloffmpeg.adapter.MusicNewAdapter;
import com.gordan.helloffmpeg.model.MusicModel;
import com.gordan.helloffmpeg.model.VideoModel;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.util.FfmpegUtil;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.OnClick;


public class MusicNet extends BaseActivity implements OnRvItemClickListener {
    final static String TAG = MusicNet.class.getSimpleName();

    @Bind(R.id.gsv_player)
    StandardGSYVideoPlayer gsyPlayer;

    @Bind(R.id.rv_music)
    RecyclerView rvMusic;

    MusicNewAdapter mAdapter;

    List<MusicModel> musicList;

    ExecutorService mExecutorService;

    FfmpegUtil mFfmpegUtil;

    VideoModel model;

    int selectedIndex = -1;

    File sdcard;

    String videoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sdcard = Environment.getExternalStorageDirectory();

        gsyPlayer.getTitleTextView().setVisibility(View.GONE);
        //设置返回键
        gsyPlayer.getBackButton().setVisibility(View.GONE);
        //设置全屏按键
        gsyPlayer.getFullscreenButton().setVisibility(View.GONE);
        //设置是否可以拖动
        gsyPlayer.setIsTouchWiget(true);

        if (getIntent() != null) {
            Intent intent = getIntent();
            model = (VideoModel) intent.getSerializableExtra("video");

            LogUtils.i(TAG, "===path:===" + model.path, false);

            gsyPlayer.setUp(model.path, true, "");
            gsyPlayer.startPlayLogic();
        }
        rvMusic.setLayoutManager(new LinearLayoutManager(this));

        musicList = new ArrayList<>();
        mExecutorService = Executors.newFixedThreadPool(2);
        mExecutorService.execute(initMusicListRunnable);

        mFfmpegUtil = new FfmpegUtil();
    }


    public Runnable initMusicListRunnable = new Runnable() {
        @Override
        public void run() {

            try {
                String[] fileNames = MusicNet.this.getAssets().list("music");

                int i = 0;

                for (String fileName : fileNames) {
                    MusicModel model = new MusicModel();
                    model.id = i;
                    model.name = fileName;
                    //目录中还要一张默认的水印的图片
                    if (fileName.endsWith("png")) {
                        continue;
                    }
                    musicList.add(model);
                    i++;
                }

                mHandler.sendEmptyMessage(Constant.MSG_MUSIC_QUERY_FINISHED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    private synchronized void convertVideo(String command) {
        mExecutorService.execute(new Runnable() {

            @Override
            public void run() {
                String[] cmd = command.split(" ");
                int result = mFfmpegUtil.convertVideoFormat(cmd);
                LogUtils.i(TAG, "=====result:" + result, false);
                mHandler.sendEmptyMessage(Constant.MSG_VIDEO_MUSIC_FINISH);
            }
        });
    }

    private void sendBroadcastMedia(String path) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path)));
        this.sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        gsyPlayer.onVideoResume();
    }


    @Override
    protected void onPause() {
        super.onPause();

        gsyPlayer.onVideoPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.FLAG_SCAN_VIDEO_REQUEST_CODE && resultCode == Constant.FLAG_SCAN_VIDEO_RESULT_CODE) {
            setResult(Constant.FLAG_SCAN_VIDEO_RESULT_CODE);
            this.finish();
        }
    }

    @Override
    protected void onDestroy() {

        //如果所有视频均释放的话 跳转播放界面就会出错 黑屏
        //GSYVideoManager.releaseAllVideos();

        //直接释放自己即可
        gsyPlayer.release();

        super.onDestroy();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_music_net;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {
            case Constant.MSG_MUSIC_QUERY_FINISHED:

                mAdapter = new MusicNewAdapter(this, musicList, R.layout.item_music_1);
                mAdapter.setOnItemClickListener(this);
                rvMusic.setAdapter(mAdapter);
                rvMusic.addItemDecoration(new LinearLayoutColorDivider(this.getResources(),
                        R.color.line, R.dimen.line, LinearLayoutManager.VERTICAL));
                break;

            case Constant.MSG_VIDEO_MUSIC_FINISH:

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                //关闭视频选择界面
                setResult(Constant.FLAG_SCAN_VIDEO_RESULT_CODE);

                //预览生成的视频
                String url = sdcard.getAbsolutePath() + File.separator + Constant.CACHE_FILE + File.separator + videoName;

                //创建的视频文件 通知系统 存储到MediaStore数据库
                sendBroadcastMedia(url);

                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("url", url);
                this.startActivity(intent);

                this.finish();
                break;
        }
    }

    MaterialDialog mProgressDialog;

    @OnClick({R.id.ll_music_native, R.id.iv_back, R.id.tv_next})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:

                this.finish();
                break;

            case R.id.tv_next:

                if(selectedIndex<0)
                {
                    showText("请先选择一首背景音乐！");
                    return;
                }

                MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(this);
                mProgressDialog = mBuilder.content("视频生成中,请稍后...").contentColor(ContextCompat.getColor(this, R.color.colorAccent))
                        .progress(true, 100, true)
                        .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
                mProgressDialog.show();

                //视频文件即 model.path

                //音乐文件
                String url = sdcard.getAbsolutePath() + File.separator + Constant.CACHE_FILE + File.separator + musicList.get(selectedIndex).name;
                //最后生成的目标视频文件
                videoName = System.currentTimeMillis() + ".mp4";
                String dest = sdcard.getAbsolutePath() + File.separator + Constant.CACHE_FILE + File.separator + videoName;

                String jniStr = "ffmpeg -i " + model.path + " -i " +
                        url + " -c:v copy -c:a mp3 -acodec libmp3lame -t 15 -strict experimental -map 0:v:0 -map 1:a:0 " + dest;
                LogUtils.i(TAG, "======jniStr=====" + jniStr, false);

                convertVideo(jniStr);

                break;

            case R.id.ll_music_native:

                Intent intent = new Intent(this, MusicNative.class);
                intent.putExtra("video", model);
                this.startActivityForResult(intent, Constant.FLAG_SCAN_VIDEO_REQUEST_CODE);

                break;
        }
    }

    @Override
    public void onItemClick(View view, int position, Object data) {
        LogUtils.i(TAG, "====onItemClick====", false);
        if (position < musicList.size()) {
            mAdapter.notifyDataChanged(position);
            selectedIndex = position;

        }
    }
}

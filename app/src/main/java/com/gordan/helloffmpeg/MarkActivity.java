package com.gordan.helloffmpeg;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.baselibrary.view.SpacesItemDecoration;
import com.gordan.helloffmpeg.adapter.VideoAdapter;
import com.gordan.helloffmpeg.model.VideoModel;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.util.FfmpegUtil;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;


/******
 * 视频水印生成得特别的慢 抖音是利用OpenGL直接渲染的
 *
 *
 * 拍摄的视频上就添加水印图片 不必再生成一个新的视频文件？
 *
 * 拍摄的视频自动就添加水印了
 *
 * *****/

public class MarkActivity extends BaseActivity implements OnRvItemClickListener,
        VideoAdapter.ItemFocusedInterface, View.OnClickListener {
    final static String TAG = MarkActivity.class.getSimpleName();

    DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    @Bind(R.id.vs_title)
    ViewStub vsTitle;

    @Bind(R.id.rv_video_list)
    RecyclerView rvListVideo;

    @Bind(R.id.gsv_player)
    StandardGSYVideoPlayer gsvPlayer;

    VideoAdapter mAdapter;

    List<VideoModel> mVideoList;

    int selectedIndex = -1;

    String input,output;

    FfmpegUtil mFfmpegUtil;

    ExecutorService mExecutorService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "=======onCreate()======");

        this.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

        View title = vsTitle.inflate();
        TextView tvTitle = (TextView) title.findViewById(R.id.tv_title);
        tvTitle.setText("本地视频");
        ((TextView)title.findViewById(R.id.tv_next)).setText("完成");
        title.findViewById(R.id.iv_back).setOnClickListener(this);
        title.findViewById(R.id.tv_next).setOnClickListener(this);

        rvListVideo.setLayoutManager(new GridLayoutManager(this, 3));

        initPlayer();

        getVideoList();

        mExecutorService = Executors.newFixedThreadPool(1);

        mFfmpegUtil = new FfmpegUtil();
    }

    @Override
    protected void onResume() {
        super.onResume();

        gsvPlayer.onVideoResume();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_mark;
    }

    @Override
    protected void handleBaseMessage(Message message) {
        Log.i(TAG, "=======handleBaseMessage()======");
        switch (message.what) {


            case Constant.MSG_MUSIC_QUERY_FINISHED:

                //Adapter中生成视频的缩略图的时候耗时太长
                mAdapter = new VideoAdapter(this, mVideoList, R.layout.item_video);
                mAdapter.setItemClickListener(this);
                mAdapter.setItemFocusedInterface(this);
                rvListVideo.setAdapter(mAdapter);
                rvListVideo.addItemDecoration(new SpacesItemDecoration(5));


                break;


            case Constant.MSG_COMMAND_EXECUTE_FINISHED:


                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                showText("处理完成！");

                sendBroadcastMedia(output);

                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("url", output);
                this.startActivity(intent);

                this.finish();

                break;
        }
    }

    private void initPlayer() {

        //String source1 = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4";
        //gsvPlayer.setUp(source1, true, "测试视频");

        //增加封面
        /*ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);
        gsvPlayer.setThumbImageView(imageView);*/
        //增加title
        gsvPlayer.getTitleTextView().setVisibility(View.GONE);
        //设置返回键
        gsvPlayer.getBackButton().setVisibility(View.GONE);
        //设置全屏按键
        gsvPlayer.getFullscreenButton().setVisibility(View.GONE);
        //是否可以滑动调整
        gsvPlayer.setIsTouchWiget(true);

        //gsvPlayer.startPlayLogic();
    }

    public void getVideoList() {

        mVideoList = new ArrayList<>();

        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        //音频文件 MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //图片文件 MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        //与网上的APP相比 该种查询数据库的方式并不能完整查询到视频

        //若有新增的视频文件无法自动更新 (只有Android系统重新启动才会自动更新)
        //通知系统进行媒体文件扫描的广播已经失效 android.intent.action.MEDIA_MOUNTED
        String[] projection = null;
        String selection = null;
        //MediaStore.Video.Media.MIME_TYPE + "=? or " + MediaStore.Video.Media.MIME_TYPE + "=?";
        String[] selectionArgs = null;
        //new String[]{"video/mp4", "video/avi"};
        String sortOrder = null;
        //MediaStore.Video.Media.DEFAULT_SORT_ORDER;
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);

        while (cursor != null && cursor.moveToNext()) {
            VideoModel model = new VideoModel();
            model.id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
            model.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
            model.path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            model.time = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
            model.duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
            LogUtils.i(TAG, "==media==" + model.path, false);
            mVideoList.add(model);
        }
        cursor.close();

        mHandler.sendEmptyMessage(Constant.MSG_MUSIC_QUERY_FINISHED);
    }


    private void sendBroadcastMedia(String path) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path)));
        this.sendBroadcast(intent);
    }

    MaterialDialog mProgressDialog;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:

                this.finish();

                break;

            case R.id.tv_next:

                if (selectedIndex < 0) {
                    return;
                }

                input = mVideoList.get(selectedIndex).path;

                MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(this);
                mProgressDialog = mBuilder.content("处理中,请稍后...")
                        .contentColor(ContextCompat.getColor(this, R.color.colorAccent)).progress(true, 100, true)
                        .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
                mProgressDialog.show();

                //获取视频源文件的后缀名
                String fileName = input.substring(input.lastIndexOf("."));
                fileName = System.currentTimeMillis() + fileName;
                Log.i(TAG, "=====fileName====" + fileName);
                output = getAppCachePath(fileName);

                //水印是应用中ASSETS目录里的一张图片 应用启动时拷贝到外部存储卡目录中的

                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {

                        String jniStr = "ffmpeg -i " + input + " -i " + getAppCachePath(Constant.WATER_MARK_DEFAULT) +
                                " -filter_complex overlay=50:" + (mDisplayMetrics.heightPixels - 150) + " " + output;

                        Log.i(TAG, "=======jniStr======" + jniStr);

                        String[] cmd = jniStr.split(" ");

                        mFfmpegUtil.convertVideoFormat(cmd);

                        mHandler.sendEmptyMessage(Constant.MSG_COMMAND_EXECUTE_FINISHED);

                    }
                });

                break;
        }
    }


    private String getAppCachePath(String filename) {
        File sdcardFile = Environment.getExternalStorageDirectory();

        return sdcardFile.getAbsolutePath() + File.separator + Constant.CACHE_FILE + File.separator + filename;
    }

    @Override
    public void onItemClick(View view, int position, Object data) {

    }

    @Override
    public void onItemFocus(int position, VideoModel model) {
        LogUtils.i(TAG, "===onItemFocus===", false);
        selectedIndex = position;

        if (gsvPlayer.isInPlayingState()) {
            gsvPlayer.onVideoReset();
        }
        gsvPlayer.setUp(model.path, true, model.title);
        gsvPlayer.startPlayLogic();
    }

    @Override
    protected void onPause() {
        super.onPause();

        gsvPlayer.onVideoPause();
    }

    @Override
    protected void onDestroy() {

        gsvPlayer.release();
        super.onDestroy();
    }
}

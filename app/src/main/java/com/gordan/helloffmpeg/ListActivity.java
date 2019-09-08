package com.gordan.helloffmpeg;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.baselibrary.view.SpacesItemDecoration;
import com.gordan.helloffmpeg.adapter.VideoAdapter;
import com.gordan.helloffmpeg.model.VideoModel;
import com.gordan.helloffmpeg.util.Constant;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class ListActivity extends BaseActivity implements View.OnClickListener,OnRvItemClickListener,
        VideoAdapter.ItemFocusedInterface
{
    final static String TAG = ListActivity.class.getSimpleName();

    @Bind(R.id.vs_title)
    ViewStub vsTitle;

    @Bind(R.id.rv_video_list)
    RecyclerView rvListVideo;

    @Bind(R.id.gsv_player)
    StandardGSYVideoPlayer gsvPlayer;

    VideoAdapter mAdapter;

    List<VideoModel> mVideoList;

    int selectedIndex=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //这里引入外部布局的时候 控件使用注入框架总是找不到 必须得用最原始的方式才能找到
        View title=vsTitle.inflate();
        TextView tvTitle=(TextView) title.findViewById(R.id.tv_title);
        tvTitle.setText("本地视频");
        title.findViewById(R.id.iv_back).setOnClickListener(this);
        title.findViewById(R.id.tv_next).setOnClickListener(this);

        mVideoList = new ArrayList<>();

        initPlayer();

        getVideoList();

        rvListVideo.setLayoutManager(new GridLayoutManager(this, 3));
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_list;
    }


    @Override
    protected void onResume() {
        super.onResume();
        gsvPlayer.onVideoResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        gsvPlayer.onVideoPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void sendBroadcastMedia()
    {
        //not allowed to send broadcast android.intent.action.MEDIA_MOUNTED from pid=17621, uid=10974
        Intent intent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.parse("file://"+ Environment.getExternalStorageDirectory()));
        //intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        this.sendBroadcast(intent);
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
            model.time=cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
            model.duration=cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
            LogUtils.i(TAG, "==media==" + model.path, false);
            mVideoList.add(model);
        }
        cursor.close();
        mHandler.sendEmptyMessage(Constant.FLAG_SCAN_VIDEO_RESULT_CODE);

    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {
            case Constant.FLAG_SCAN_VIDEO_RESULT_CODE:

                //Adapter中生成视频的缩略图的时候耗时太长
                mAdapter = new VideoAdapter(this, mVideoList, R.layout.item_video);
                mAdapter.setItemClickListener(this);
                mAdapter.setItemFocusedInterface(this);
                rvListVideo.setAdapter(mAdapter);
                rvListVideo.addItemDecoration(new SpacesItemDecoration(5));

                break;
        }

    }


    @Override
    public void onClick(View v) {


        switch (v.getId())
        {
            case R.id.iv_back:

                onBackPressed();

                break;

            case R.id.tv_next:

                VideoModel videoModel=mVideoList.get(selectedIndex);
                if(videoModel!=null)
                {
                    Intent intent=new Intent(this,MusicNet.class);
                    intent.putExtra("video",videoModel);
                    startActivityForResult(intent,Constant.FLAG_SCAN_VIDEO_REQUEST_CODE);
                }



                break;
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.i(TAG,requestCode+"===onActivityResult===="+resultCode,false);

        if(requestCode==Constant.FLAG_SCAN_VIDEO_REQUEST_CODE && resultCode==Constant.FLAG_SCAN_VIDEO_RESULT_CODE)
        {
            this.finish();
        }
    }

    @Override
    public void onBackPressed() {
        LogUtils.i(TAG, "==onBackPressed==", false);

        //释放所有

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LogUtils.i(TAG, "==onDestroy==", false);
        super.onDestroy();
        GSYVideoManager.releaseAllVideos();
    }


    @Override
    public void onItemClick(View view, int position, Object data) {
        LogUtils.i(TAG, "===onItemClick===", false);


    }


    @Override
    public void onItemFocus(int position, VideoModel model) {

        LogUtils.i(TAG, "===onItemFocus===", false);
        selectedIndex=position;

        if (gsvPlayer.isInPlayingState()) {
            gsvPlayer.onVideoReset();
        }
        gsvPlayer.setUp(model.path, true, model.title);
        gsvPlayer.startPlayLogic();
    }

}

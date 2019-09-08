package com.gordan.helloffmpeg;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.OnClick;

public class MusicNative extends BaseActivity implements OnRvItemClickListener
{
    final static String TAG=MusicNative.class.getSimpleName();

    @Bind(R.id.rv_music_native)
    RecyclerView rvMusic;
    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.et_keyword)
    EditText etKeyword;
    @Bind(R.id.iv_search)
    ImageView ivSearch;

    MusicNewAdapter mAdapter;

    List<MusicModel> musicList;

    VideoModel model;

    ExecutorService mExecutorService;

    FfmpegUtil mFfmpegUtil;

    File sdcard;

    int selectedIndex=-1;

    String videoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getIntent()!=null)
        {
            Intent intent=getIntent();
            model=(VideoModel) intent.getSerializableExtra("video");
        }

        sdcard= Environment.getExternalStorageDirectory();

        rvMusic.setLayoutManager(new LinearLayoutManager(this));

        mExecutorService= Executors.newFixedThreadPool(2);
        mFfmpegUtil=new FfmpegUtil();

        getAudioList();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_music_native;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what)
        {
            case Constant.MSG_MUSIC_QUERY_FINISHED:

                mAdapter=new MusicNewAdapter(this,musicList,R.layout.item_music_1);
                mAdapter.setOnItemClickListener(this);
                rvMusic.setAdapter(mAdapter);
                rvMusic.addItemDecoration(new LinearLayoutColorDivider(this.getResources(),
                        R.color.line,R.dimen.line,LinearLayoutManager.VERTICAL));
                break;

            case Constant.MSG_VIDEO_MUSIC_FINISH:

                if(mProgressDialog!=null)
                {
                    mProgressDialog.dismiss();
                    mProgressDialog=null;
                }

                setResult(Constant.FLAG_SCAN_VIDEO_RESULT_CODE);

                //预览生成的视频
                String url=sdcard.getAbsolutePath()+File.separator+Constant.CACHE_FILE+File.separator+videoName;

                //创建的视频文件通知系统存储到MediaStore数据库里
                sendBroadcastMedia(url);

                Intent intent=new Intent(this,PlayerActivity.class);
                intent.putExtra("url",url);
                this.startActivity(intent);

                this.finish();

                break;
        }

    }

    private void sendBroadcastMedia(String path)
    {
        Intent intent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(new File(path)));
        this.sendBroadcast(intent);
    }



    public void getAudioList() {
        musicList = new ArrayList<>();
        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        while (cursor != null && cursor.moveToNext()) {

            MusicModel model = new MusicModel();

            model.id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            model.path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            // cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
            model.name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            model.time = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));
            model.duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

            LogUtils.i(TAG, "===path===" + model.path, false);

            musicList.add(model);
        }
        cursor.close();
        mHandler.sendEmptyMessage(Constant.MSG_MUSIC_QUERY_FINISHED);
    }

    @Override
    public void onItemClick(View view, int position, Object data) {
        LogUtils.i(TAG,"===onItemClick===",false);
        if(position<musicList.size())
        {
            mAdapter.notifyDataChanged(position);
            selectedIndex=position;
        }
    }

    MaterialDialog mProgressDialog;

    @OnClick({R.id.iv_search,R.id.tv_next})
    public void onViewClick(View view)
    {
        switch (view.getId())
        {
            case R.id.iv_search:

                etKeyword.setVisibility(View.VISIBLE);
                tvTitle.setVisibility(View.GONE);

                break;

            case R.id.tv_next:

                if(selectedIndex<0)
                {
                    showText("请先选择一首背景音乐！");
                    return;
                }

                MaterialDialog.Builder mBuilder=new MaterialDialog.Builder(this);
                mProgressDialog=mBuilder.content("视频生成中,请稍后...").contentColor(ContextCompat.getColor(this,R.color.colorAccent))
                        .progress(true,100,true)
                        .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
                mProgressDialog.show();

                //视频文件即 model.path

                //音乐文件
                MusicModel music=musicList.get(selectedIndex);
                //最后生成的目标视频文件
                videoName=System.currentTimeMillis()+".mp4";
                String dest=sdcard.getAbsolutePath()+File.separator+Constant.CACHE_FILE+File.separator+videoName;

                //本地音乐名字中往往包含有空格 如果直接使用字符串的形式一次性切割会出错
                String[] cmd=new String[20];

                cmd[0]="ffmpeg";
                cmd[1]="-i";
                cmd[2]=model.path;
                cmd[3]="-i";
                cmd[4]=music.path;

                cmd[5]="-c:v";
                cmd[6]="copy";
                cmd[7]="-c:a";
                cmd[8]="mp3";
                cmd[9]="-acodec";

                cmd[10]="libmp3lame";
                cmd[11]="-t";
                cmd[12]="15";
                cmd[13]="-strict";
                cmd[14]="experimental";

                cmd[15]="-map";
                cmd[16]="0:v:0";
                cmd[17]="-map";
                cmd[18]="1:a:0";
                cmd[19]=dest;

                convertVideo(cmd);

                break;
        }
    }


    private synchronized void convertVideo(String[] command)
    {
        mExecutorService.execute(new Runnable() {

            @Override
            public void run()
            {
                LogUtils.i(TAG,"======ready=====",false);
                int result= mFfmpegUtil.convertVideoFormat(command);
                LogUtils.i(TAG,"=====result:"+result,false);
                mHandler.sendEmptyMessage(Constant.MSG_VIDEO_MUSIC_FINISH);
            }
        });
    }
}

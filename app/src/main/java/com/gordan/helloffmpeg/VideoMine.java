package com.gordan.helloffmpeg;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.baselibrary.view.SpacesItemDecoration;
import com.gordan.helloffmpeg.adapter.VideoNewAdapter;
import com.gordan.helloffmpeg.model.VideoModel;
import com.gordan.helloffmpeg.util.Constant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.OnClick;


/******
 * 采用 MediaScannerConnection 扫描的方式 跟遍历文件的方式类似 都需要筛选文件的后缀名，更严重的问题是无法获取视频的时长
 *
 * 视频文件可能出现和SD卡中的视频文件不同步的情况，终极解决办法是重启系统
 * 通知系统进行媒体文件扫描的广播已经失效 android.intent.action.MEDIA_MOUNTED，只有等到用户自己重启系统
 *
 * 读取MediaStore数据库中的视频文件 根据视频文件的绝对路径去筛选 当前应用的视频
 * 应用缓存的视频文件的路径一定是以 /storage/emulated/0/gordanxu/ 开头的
 *
 * 删除缓存的视频文件以后 MediaStore数据库中的记录也应该同时删除
 *
 * 是否可以添加一个服务自己扫描外部存储卡 将未添加到数据的单个文件更新到MediaStore数据库中？
 *
 */
public class VideoMine extends BaseActivity implements OnRvItemClickListener, VideoNewAdapter.ItemSelectInterface {
    final static String TAG = VideoMine.class.getSimpleName();

    @Bind(R.id.rv_video)
    RecyclerView rvMineVideo;
    @Bind(R.id.tv_edit)
    TextView tvEdit;

    VideoNewAdapter mNewAdapter;

    File sdcard;

    List<VideoModel> mVideoList;

    ExecutorService mExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sdcard = Environment.getExternalStorageDirectory();

        rvMineVideo.setLayoutManager(new GridLayoutManager(this, 2));

        //mExecutorService = Executors.newFixedThreadPool(2);

        getVideoList();
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_video_mine;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {
            case Constant.MSG_MUSIC_QUERY_FINISHED:

                mNewAdapter = new VideoNewAdapter(this, mVideoList, R.layout.item_mine_video);
                mNewAdapter.setItemClickListener(this);
                mNewAdapter.setItemSelectListener(this);
                rvMineVideo.setAdapter(mNewAdapter);

                rvMineVideo.addItemDecoration(new SpacesItemDecoration(10));

                break;
        }
    }

    public synchronized void getVideoList() {

        mVideoList = new ArrayList<>();

        ContentResolver contentResolver = this.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        //出现过 视频文件和图片也扫描出来了的情况
        File cache = new File(sdcard, Constant.CACHE_FILE);

        String[] projection = null;
        String selection = MediaStore.Video.Media.DATA + " like ?";
        String[] selectionArgs = new String[]{cache.getAbsolutePath() + "%"};
        String sortOrder = MediaStore.Video.Media.DEFAULT_SORT_ORDER;
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


    public synchronized void setModelStatus(int status) {
        for (VideoModel model : mVideoList) {
            model.editFlag = status;
        }
    }

    public synchronized void deleteVideo() {
        for (VideoModel model : mVideoList) {
            if (model.editFlag == 2) {
                File temp = new File(model.path);
                if (temp.exists()) {
                    LogUtils.i(TAG, "=====VideoModel====" + temp.delete(), false);
                }

                //同步删除MediaStore数据库中的记录
                deleteMediaStoreData(model.path);
            }
        }

        getVideoList();
    }

    private synchronized void deleteMediaStoreData(String path) {
        String where = MediaStore.Video.Media.DATA + " = '" + path + "'";
        this.getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, where, null);
    }


    private int getDelCount() {
        int sum = 0;
        for (VideoModel model : mVideoList) {
            if (model.editFlag == 2) {
                sum++;
            }
        }
        return sum;
    }

    @Override
    public void onItemSelected(int position, int status) {
        if (position < mVideoList.size()) {
            mVideoList.get(position).editFlag = status;
            mNewAdapter.notifyDataChanged(mVideoList);
        }
    }

    @Override
    public void onItemClick(View view, int position, Object data) {
        LogUtils.i(TAG, "===onItemClick===" + position, false);
        if (position < mVideoList.size()) {
            VideoModel model = mVideoList.get(position);

            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("url", model.path);
            this.startActivity(intent);
        }
    }

    MaterialDialog confirmDialog;

    private void showDelConfirmDialog() {
        MaterialDialog.Builder mBuilder = new MaterialDialog.Builder(this);

        confirmDialog = mBuilder.content("确定删除选中的视频？").contentColor(ContextCompat.getColor(this, R.color.colorAccent))
                .positiveText("确定").positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        deleteVideo();

                        confirmDialog.dismiss();
                        confirmDialog = null;

                        //setModelStatus(0);
                        tvEdit.setTag("0");
                        tvEdit.setText("编辑");

                    }
                }).negativeText("取消").negativeColor(ContextCompat.getColor(this, R.color.colorAccent))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        confirmDialog.dismiss();
                        confirmDialog = null;

                        setModelStatus(0);
                        tvEdit.setTag("0");
                        tvEdit.setText("编辑");

                    }
                }).build();

        confirmDialog.show();
    }


    @OnClick({R.id.iv_back,R.id.tv_edit})
    public void onViewClick(View view) {
        switch (view.getId()) {

            case R.id.iv_back:

                this.finish();

                break;

            case R.id.tv_edit:

                String status = tvEdit.getTag() + "";
                if ("1".equalsIgnoreCase(status)) {
                    //统计选中的视频 并且执行删除
                    if (getDelCount() > 0) {
                        showDelConfirmDialog();
                    } else {
                        //未选中任何视频 恢复至默认状态即可
                        setModelStatus(0);
                        tvEdit.setTag("0");
                        tvEdit.setText("编辑");
                    }
                } else {
                    setModelStatus(1);
                    tvEdit.setTag("1");
                    tvEdit.setText("删除");
                }
                mNewAdapter.notifyDataChanged(mVideoList);

                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.i(TAG, "===onDestroy===", false);
    }
}

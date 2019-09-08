package com.gordan.helloffmpeg.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.gordan.baselibrary.easyadapter.EasyRVAdapter;
import com.gordan.baselibrary.easyadapter.EasyRVHolder;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.baselibrary.util.LogUtils;
import com.gordan.helloffmpeg.R;
import com.gordan.helloffmpeg.model.VideoModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VideoAdapter extends EasyRVAdapter<VideoModel> {

    final static String TAG=VideoAdapter.class.getSimpleName();

    private OnRvItemClickListener listener;

    Context mContext;

    SimpleDateFormat sdfDuration,sdfTime;

    /***
     * 这种焦点框的效果如果使用notify的方式去做响应将会很慢
     *
     * 焦点框的效果在TV应用种常见，移植到手机上时需要设置  触摸时获取焦点  的属性设置为true 还要可点击/可获取焦点
     *
     * 点击视频播放就会存在问题（Item需要点击两次）
     *
     * 触发Item的Click事件需要点击两次，可将Item获取焦点的事件传递出来
     *
     *
     * 使用MediaMetadataRetriever类获取缩略图将会很慢，还是建议使用查询媒体库的方式
     *
     * ***/

    public VideoAdapter(Context context, List<VideoModel> list, int... layoutIds) {
        super(context, list, layoutIds);
        this.mContext = context;
        sdfDuration=new SimpleDateFormat("mm:ss");
        sdfTime=new SimpleDateFormat("MM-dd");
    }

    @Override
    protected void onBindData(EasyRVHolder viewHolder, int position, VideoModel item) {

        if (TextUtils.isEmpty(item.path)) {

            return;
        }

        String title = item.path.substring(item.path.lastIndexOf("/") + 1);
        String duration=sdfDuration.format(new Date(item.duration));
        String time=sdfTime.format(new Date(item.time));
        //视频的标题  设置视频的缩略图
        viewHolder.setText(R.id.tv_video_title, title).setText(R.id.tv_duration,duration).setText(R.id.tv_time,time);

        Bitmap temBitmap=getVideoCover(item.id);
        if(temBitmap!=null)
        {
            viewHolder.setImageBitmap(R.id.iv_video_cover,temBitmap);
        }
        else
        {
            //调试的过程中发现查询视频的缩略图 有些查询到的为空
            ImageView iv=viewHolder.itemView.findViewById(R.id.iv_video_cover);
            Glide.with(mContext).load(Uri.fromFile(new File(item.path))).into(iv);
        }


        if (this.listener != null) {
            viewHolder.setOnItemViewClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    listener.onItemClick(viewHolder.itemView, position, item);

                }
            });
        }


        //将Item获取焦点的回调传递出来
        if(focusedListener!=null)
        {
            viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {


                    LogUtils.i(TAG,"===onFocusChange==="+hasFocus,false);

                    if(hasFocus)
                    {
                        focusedListener.onItemFocus(position,item);
                    }

                }
            });
        }


    }

    public void setItemClickListener(OnRvItemClickListener listener) {
        this.listener = listener;
    }

    public Bitmap getVideoCover(long id)
    {
        Bitmap bitmap=null;
        String albumPath = "";
        Cursor thumbCursor = this.mContext.getApplicationContext().getContentResolver().query(
                MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                null, MediaStore.Video.Thumbnails.VIDEO_ID
                        + "=" + id, null, null);
        if (thumbCursor!=null && thumbCursor.moveToFirst()) {
            albumPath = thumbCursor.getString(thumbCursor
                    .getColumnIndex(MediaStore.Video.Thumbnails.DATA));
            bitmap = BitmapFactory.decodeFile(albumPath);
        }

        thumbCursor.close();
        return bitmap;
    }


    public Bitmap getVideoThumbNail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }


    private ItemFocusedInterface focusedListener;

    public void setItemFocusedInterface(ItemFocusedInterface listener)
    {
        this.focusedListener=listener;
    }

    public interface ItemFocusedInterface
    {
        void onItemFocus(int position,VideoModel model);
    }

}

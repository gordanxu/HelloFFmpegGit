package com.gordan.helloffmpeg.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
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

public class VideoNewAdapter extends EasyRVAdapter<VideoModel> {

    final static String TAG= VideoNewAdapter.class.getSimpleName();

    private OnRvItemClickListener listener;

    private ItemSelectInterface selectListener;

    Context mContext;

    SimpleDateFormat sdfDuration,sdfTime;

    public VideoNewAdapter(Context context, List<VideoModel> list, int... layoutIds) {
        super(context, list, layoutIds);
        this.mContext = context;
        sdfDuration=new SimpleDateFormat("mm:ss");
        sdfTime=new SimpleDateFormat("MM-dd");
    }

    public void notifyDataChanged(List<VideoModel> data)
    {
        this.mList=data;
        this.notifyDataSetChanged();
    }

    @Override
    protected void onBindData(EasyRVHolder viewHolder, int position, VideoModel item) {

        if (TextUtils.isEmpty(item.path)) {

            return;
        }

        String title = item.path.substring(item.path.lastIndexOf("/") + 1);
        String duration=sdfDuration.format(new Date(item.duration));
        String time=sdfTime.format(new Date(item.time));
        //视频的标题  视频的缩略图
        viewHolder.setText(R.id.tv_video_title, title).setText(R.id.tv_duration,duration).setText(R.id.tv_time,time);
        Bitmap temBitmap=getVideoCover(item.id);
        if(temBitmap!=null)
        {
            viewHolder.setImageBitmap(R.id.iv_video_cover,temBitmap);
        }
        else
        {
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

        ImageView ivStatus=(ImageView) viewHolder.itemView.findViewById(R.id.iv_item_status);
        if(item.editFlag>0)
        {
            //编辑状态
            ivStatus.setVisibility(View.VISIBLE);
            if(item.editFlag==1)
            {
                //编辑状态时未选中
                ivStatus.setImageDrawable(ContextCompat.getDrawable(mContext,R.drawable.icon_item_default));
            }
            else
            {
                //编辑状态时选中
                ivStatus.setImageDrawable(ContextCompat.getDrawable(mContext,R.drawable.icon_item_selected));
            }

            ivStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    String tag=ivStatus.getTag()==null?"1":(ivStatus.getTag()+"");
                    LogUtils.i(TAG,"===onClick()==="+tag,false);
                    if("2".equalsIgnoreCase(tag))
                    {
                        ivStatus.setTag("1");
                        tag="1";
                    }
                    else
                    {
                        ivStatus.setTag("2");
                        tag="2";
                    }

                    if(selectListener!=null)
                    {
                        int status=Integer.parseInt(tag);
                        selectListener.onItemSelected(position,status);
                    }
                }
            });
        }
        else
        {
            //默认状态
            ivStatus.setVisibility(View.GONE);
        }
    }

    public void setItemClickListener(OnRvItemClickListener listener) {
        this.listener = listener;
    }

    public void setItemSelectListener(ItemSelectInterface selectListener)
    {
        this.selectListener=selectListener;
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


    public interface ItemSelectInterface
    {
        void onItemSelected(int position,int status);
    }


}

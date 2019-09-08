package com.gordan.helloffmpeg.adapter;

import android.content.Context;
import android.view.View;

import com.gordan.baselibrary.easyadapter.EasyRVAdapter;
import com.gordan.baselibrary.easyadapter.EasyRVHolder;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.helloffmpeg.R;
import com.gordan.helloffmpeg.model.MusicModel;

import java.util.List;

public class MusicNewAdapter extends EasyRVAdapter<MusicModel>
{
    private int selectedIndex=-1;

    private OnRvItemClickListener listener;

    public MusicNewAdapter(Context context, List<MusicModel> list, int... layoutIds) {
        super(context, list, layoutIds);
    }

    @Override
    protected void onBindData(EasyRVHolder viewHolder, int position, MusicModel item) {

        viewHolder.setText(R.id.tv_music_name,item.name);

        if(selectedIndex>=0)
        {
            if (selectedIndex == position) {
                viewHolder.setVisible(R.id.iv_status,View.VISIBLE);
            }
            else
            {
                viewHolder.setVisible(R.id.iv_status,View.INVISIBLE);
            }
        }



        if(this.listener!=null)
        {
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(viewHolder.itemView,position,item);
                }
            });
        }

    }

    public void setOnItemClickListener(OnRvItemClickListener listener)
    {
        this.listener=listener;
    }

    public void notifyDataChanged(int index)
    {
        this.selectedIndex=index;
        this.notifyDataSetChanged();
    }
}

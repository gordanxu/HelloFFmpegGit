package com.gordan.helloffmpeg.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gordan.helloffmpeg.R;
import com.gordan.helloffmpeg.model.MusicModel;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MusicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mContext;

    List<MusicModel> musicList;

    int mResId;


    public MusicAdapter(Context context, List<MusicModel> data, int resId) {
        super();
        this.mContext = context;
        this.musicList = data;
        this.mResId = resId;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View contentView = LayoutInflater.from(viewGroup.getContext()).inflate(mResId, viewGroup, false);

        return new MusicViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

        MusicModel model = musicList.get(i);

        MusicViewHolder myHolder = (MusicViewHolder) viewHolder;

        myHolder.tvName.setText(model.name);

        if (this.listener != null) {

            myHolder.ivPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(viewHolder.itemView, viewHolder.getLayoutPosition());
                }
            });

            myHolder.ivUse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemUseClick(viewHolder.itemView,viewHolder.getLayoutPosition());
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    private ItemClickInterface listener;

    public void setItemClickListener(ItemClickInterface listener) {
        this.listener = listener;
    }

    public interface ItemClickInterface {
        public void onItemClick(View item, int position);
        public void onItemUseClick(View item, int position);

    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.tv_music_name)
        TextView tvName;
        @Bind(R.id.iv_play)
        ImageView ivPlay;
        @Bind(R.id.iv_status)
        ImageView ivUse;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

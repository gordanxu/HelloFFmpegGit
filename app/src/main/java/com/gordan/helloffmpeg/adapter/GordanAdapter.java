package com.gordan.helloffmpeg.adapter;

import android.content.Context;
import android.view.View;

import com.gordan.baselibrary.easyadapter.EasyRVAdapter;
import com.gordan.baselibrary.easyadapter.EasyRVHolder;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.helloffmpeg.R;
import com.gordan.helloffmpeg.model.GordanModel;

import java.util.List;

public class GordanAdapter extends EasyRVAdapter<GordanModel> {
    private OnRvItemClickListener listener;

    public GordanAdapter(Context context, List<GordanModel> list, int... layoutIds) {
        super(context, list, layoutIds);
    }

    public void setItemClickListener(OnRvItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onBindData(EasyRVHolder viewHolder, int position, GordanModel item) {

        if (item.id > 0) {

            //加载圆角图片
            //viewHolder.setRoundImageUrl(R.id.iv_movie_cover,item.movieCover,R.drawable.apple);

            //加载圆形图片（有个问题是：若把ImageView的宽高设置为固定值 感觉圆形图片还是被压缩变型了 不是真正的圆形）
            //将Glide版本升级到最新版就正常了~~
            viewHolder.setCircleImageUrl(R.id.iv_movie_cover,item.movieCover,R.drawable.apple);

            viewHolder.setText(R.id.tv_movie_name, item.movieName)
                    .setText(R.id.tv_director_name, item.directorName);
        }


        if (this.listener != null) {
            viewHolder.setOnItemViewClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    listener.onItemClick(viewHolder.itemView, position, item);

                }
            });

        }


    }
}

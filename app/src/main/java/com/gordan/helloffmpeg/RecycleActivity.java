package com.gordan.helloffmpeg;

import android.os.Message;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.baselibrary.easyadapter.glide.GlideCircleTransform;
import com.gordan.baselibrary.easyadapter.helper.OnRvItemClickListener;
import com.gordan.helloffmpeg.adapter.GordanAdapter;
import com.gordan.helloffmpeg.model.GordanModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class RecycleActivity extends BaseActivity implements OnRvItemClickListener<GordanModel>

{
    @Bind(R.id.rv_movie)
    RecyclerView rvMovie;
    @Bind(R.id.iv_test_img)
    ImageView ivTest;

    GordanAdapter mGordanAdapter;

    List<GordanModel> mListMovie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url="http://img5.mtime.cn/CMS/News/2019/08/20/100109.35393806_100X100.jpg";

        Glide.with(this).load(url).placeholder(R.drawable.apple).transform(new GlideCircleTransform()).into(ivTest);

        initData();
    }

    private void initData()
    {
        rvMovie.setLayoutManager(new LinearLayoutManager(this));

        mListMovie=new ArrayList<>();

        int size=5;

        //http://img5.mtime.cn/CMS/News/2019/08/19/113727.75378529_620X620.jpg
        for (int i=0;i<size;i++)
        {
            GordanModel model=new GordanModel();
            model.id=(i+1);
            model.movieCover="http://img5.mtime.cn/CMS/News/2019/08/20/100109.35393806_100X100.jpg";
            model.movieName="魔童降世之哪吒";
            model.directorName="饺子导演";

            mListMovie.add(model);
        }

        mGordanAdapter=new GordanAdapter(this,mListMovie,R.layout.item_gordan);
        mGordanAdapter.setItemClickListener(this);
        rvMovie.setAdapter(mGordanAdapter);

    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_recycle;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @Override
    public void onItemClick(View view, int position, GordanModel data) {

        showText("点击了Item:"+position);

    }


}

package com.gordan.helloffmpeg.fragment;

import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.View;

import com.gordan.baselibrary.fragment.BaseFragment;
import com.gordan.helloffmpeg.CameraActivity;
import com.gordan.helloffmpeg.PlayerActivity;
import com.gordan.helloffmpeg.R;

import java.io.File;

import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 */
public class ImageFragment extends BaseFragment {


    public ImageFragment() {
        // Required empty public constructor
    }


    @Override
    protected int inflateResId() {
        return R.layout.fragment_image;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void handlerBaseMessage(Message message) {

    }

    @OnClick({R.id.ll_module_play,R.id.ll_module_picture})
    public void onViewClick(View view)
    {
        switch (view.getId())
        {
            case R.id.ll_module_play:


                File sdcardFile = Environment.getExternalStorageDirectory();

                String url=sdcardFile.getAbsolutePath()+ File.separator+"output.mp4";

                Intent intent=new Intent(mActivity,PlayerActivity.class);
                intent.putExtra("url",url);
                this.startActivity(intent);


                break;

            case R.id.ll_module_picture:

                intent=new Intent(mActivity, CameraActivity.class);
                startActivity(intent);

                break;
        }
    }



}

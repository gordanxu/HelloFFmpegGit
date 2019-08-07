package com.gordan.helloffmpeg.fragment;

import android.content.Intent;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.View;

import com.gordan.baselibrary.fragment.BaseFragment;
import com.gordan.helloffmpeg.CameraActivity;
import com.gordan.helloffmpeg.DecodeActivity;
import com.gordan.helloffmpeg.MainActivity;
import com.gordan.helloffmpeg.MarkActivity;
import com.gordan.helloffmpeg.R;

import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class VideoFragment extends BaseFragment {


    public VideoFragment() {
        // Required empty public constructor
    }


    @Override
    protected int inflateResId() {
        return R.layout.fragment_video;
    }

    @Override
    protected void handlerBaseMessage(Message message) {

    }

    @Override
    protected void initData() {

    }

    @OnClick({R.id.ll_module_video,R.id.ll_module_mark, R.id.ll_module_v_yuv, R.id.ll_module_tools})
    public void onViewClick(View view) {
        switch (view.getId()) {

            case R.id.ll_module_video:

                Intent intent = new Intent(mActivity, CameraActivity.class);
                startActivity(intent);
                break;


            case R.id.ll_module_v_yuv:
                intent = new Intent(mActivity, DecodeActivity.class);
                startActivity(intent);
                break;


            case R.id.ll_module_mark:
                intent = new Intent(mActivity, MarkActivity.class);
                startActivity(intent);
                break;

            case R.id.ll_module_tools:

                intent = new Intent(mActivity, MainActivity.class);
                startActivity(intent);

                break;
        }
    }


}

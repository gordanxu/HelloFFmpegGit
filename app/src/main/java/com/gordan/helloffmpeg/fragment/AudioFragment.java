package com.gordan.helloffmpeg.fragment;

import android.content.Intent;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.View;

import com.gordan.baselibrary.fragment.BaseFragment;
import com.gordan.helloffmpeg.AudioActivity;
import com.gordan.helloffmpeg.MergeActivity;
import com.gordan.helloffmpeg.R;

import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class AudioFragment extends BaseFragment {


    public AudioFragment() {
        // Required empty public constructor
    }

    @Override
    protected int inflateResId() {
        return R.layout.fragment_audio;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void handlerBaseMessage(Message message) {

    }


    @OnClick({R.id.ll_module_pcm, R.id.ll_module_merge})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.ll_module_pcm:

                Intent intent = new Intent(mActivity, AudioActivity.class);
                startActivity(intent);

                break;

            case R.id.ll_module_merge:

                intent = new Intent(mActivity, MergeActivity.class);
                startActivity(intent);

                break;
        }
    }


}

package com.gordan.baselibrary.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.ButterKnife;

public abstract class BaseFragment extends Fragment {
    protected Activity mActivity;

    protected View mFragmentView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    Handler mFragmentHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            handlerBaseMessage(msg);

            return false;
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mFragmentView = inflater.inflate(inflateResId(), null);
        ButterKnife.bind(this,mFragmentView);

        initData();

        return mFragmentView;
    }

    protected abstract int inflateResId();

    protected abstract void initData();

    protected abstract void handlerBaseMessage(Message message);

    protected void showToastMessage(String message)
    {
        Toast.makeText(mActivity,message,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        ButterKnife.unbind(this);
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        mFragmentHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

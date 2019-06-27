package com.gordan.helloffmpeg;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import butterknife.ButterKnife;

public abstract class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int resId=inflateResId();
        if(resId>0)
        {
            setContentView(resId);
            ButterKnife.bind(this);
        }
    }

    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            handleBaseMessage(msg);

            return false;
        }
    });

    protected abstract int inflateResId();

    protected abstract void handleBaseMessage(Message message);

    protected void showText(String text)
    {
        Toast.makeText(this,text,Toast.LENGTH_LONG).show();
    }

    protected void gone(final View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }

    protected void visible(final View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    protected boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }


    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        ButterKnife.unbind(this);
        super.onDestroy();
    }
}

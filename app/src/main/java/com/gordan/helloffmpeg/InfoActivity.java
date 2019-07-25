package com.gordan.helloffmpeg;


import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.widget.TextView;

import com.gordan.baselibrary.BaseActivity;

import butterknife.Bind;

public class InfoActivity extends BaseActivity {

    @Bind(R.id.tv_info)
    TextView tvInfo;

    String content = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Intent intent = getIntent();
            content = intent.getStringExtra("content");
            tvInfo.setText(content);
        }

    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_info;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }
}

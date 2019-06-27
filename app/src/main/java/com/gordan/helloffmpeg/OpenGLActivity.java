package com.gordan.helloffmpeg;

import android.app.Activity;
import android.os.Bundle;

import com.gordan.helloffmpeg.view.GordanGLSurfaceView;

public class OpenGLActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_open_gl);

        GordanGLSurfaceView gordanSurfaceView=new GordanGLSurfaceView(this);
        setContentView(gordanSurfaceView);
    }
}

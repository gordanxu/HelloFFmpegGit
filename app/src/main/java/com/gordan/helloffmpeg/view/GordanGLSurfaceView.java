package com.gordan.helloffmpeg.view;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GordanGLSurfaceView extends GLSurfaceView
{

    public GordanGLSurfaceView(Context context)
    {
        super(context);

        setEGLContextClientVersion(2);

        GordanRender mRender=new GordanRender();

        setRenderer(mRender);

    }

}

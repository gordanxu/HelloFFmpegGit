package com.gordan.helloffmpeg.filter;

import android.opengl.GLES20;
import android.util.Log;

import com.gordan.helloffmpeg.util.EasyGlUtils;


/**
 * Created by cj on 2017/7/20 0020.
 * 滑动切换滤镜的控制类
 */

public class SlideGpuFilterGroup {

    final static String TAG=SlideGpuFilterGroup.class.getSimpleName();

    private GPUImageFilter curFilter;
    private GPUImageFilter leftFilter;
    private GPUImageFilter rightFilter;
    private int width, height;
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];

    private OnFilterChangeListener mListener;

    public SlideGpuFilterGroup() {
        initFilter();
    }

    private void initFilter() {
        curFilter = new MagicAntiqueFilter();
        leftFilter = new MagicCoolFilter();
        rightFilter = new MagicN1977Filter();
    }


    public void init() {
        curFilter.init();
        leftFilter.init();
        rightFilter.init();
    }

    public void onSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
        GLES20.glGenFramebuffers(1, fFrame, 0);
        EasyGlUtils.genTexturesWithParameter(1, fTexture, 0, GLES20.GL_RGBA, width, height);
        onFilterSizeChanged(width, height);
    }

    private void onFilterSizeChanged(int width, int height) {
        curFilter.onInputSizeChanged(width, height);
        leftFilter.onInputSizeChanged(width, height);
        rightFilter.onInputSizeChanged(width, height);
        curFilter.onDisplaySizeChanged(width, height);
        leftFilter.onDisplaySizeChanged(width, height);
        rightFilter.onDisplaySizeChanged(width, height);
    }

    public int getOutputTexture() {
        return fTexture[0];
    }

    public void onDrawFrame(int textureId,int filterIndex) {

        Log.i(TAG,"====onDrawFrame()===="+filterIndex);
        if(filterIndex<0)
        {
            Log.i(TAG,"====no filter so return===");
            return;
        }

        EasyGlUtils.bindFrameTexture(fFrame[0], fTexture[0]);

        if(filterIndex==0)
        {
            curFilter.onDrawFrame(textureId);
        }
        else if(filterIndex==1)
        {
            leftFilter.onDrawFrame(textureId);
        }
        else
        {
            rightFilter.onDrawFrame(textureId);
        }
        EasyGlUtils.unBindFrameBuffer();
    }


    public void destroy() {
        curFilter.destroy();
        leftFilter.destroy();
        rightFilter.destroy();
    }





    public void setOnFilterChangeListener(OnFilterChangeListener listener) {
        this.mListener = listener;
    }

    public interface OnFilterChangeListener {
        void onFilterChange(int type);
    }
}

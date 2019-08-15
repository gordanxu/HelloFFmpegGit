package com.gordan.helloffmpeg.view;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.gordan.helloffmpeg.filter.AFilter;
import com.gordan.helloffmpeg.filter.CameraFilter;
import com.gordan.helloffmpeg.filter.GroupFilter;
import com.gordan.helloffmpeg.filter.NoFilter;
import com.gordan.helloffmpeg.filter.SlideGpuFilterGroup;
import com.gordan.helloffmpeg.util.EasyGlUtils;
import com.gordan.helloffmpeg.util.MatrixUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 *
 *
 * 主要用于管理各种滤镜、画面旋转、视频编码录制等
 */

public class CameraDrawer implements GLSurfaceView.Renderer {

    final static String TAG = CameraDrawer.class.getSimpleName();

    private float[] OM;
    /**
     * 显示画面的filter
     */
    private final AFilter showFilter;
    private final AFilter drawFilter;

    private final GroupFilter mAfFilter;

    private SlideGpuFilterGroup mSlideFilterGroup;

    private SurfaceTexture mSurfaceTextrue;
    /**
     * 预览数据的宽高
     */
    private int mPreviewWidth = 0, mPreviewHeight = 0;
    /**
     * 控件的宽高
     */
    private int width = 0, height = 0;

    private int textureID;
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];

    private float[] SM = new float[16];     //用于显示的变换矩阵

    private int filterIndex = -1;

    public CameraDrawer(Resources resources) {
        //初始化一个滤镜 也可以叫控制器
        showFilter = new NoFilter(resources);
        drawFilter = new CameraFilter(resources);
        mAfFilter = new GroupFilter(resources);
        mSlideFilterGroup = new SlideGpuFilterGroup();

        //必须传入上下翻转的矩阵
        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM, false, true);//矩阵上下翻转
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.i(TAG, "=====onSurfaceCreated()=====");
        textureID = createTextureID();
        mSurfaceTextrue = new SurfaceTexture(textureID);

        showFilter.create();
        //showFilter.setTextureId(textureID);

        drawFilter.create();
        drawFilter.setTextureId(textureID);

        //必须传入上下翻转的矩阵
        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM, false, true);//矩阵上下翻转

        //非必需
        showFilter.setMatrix(OM);

        /** 解决预览画面上下反转的问题
         *
         * 上下的反转是因为纹理坐标和 屏幕坐标的原点不同而引起的吗？
         *
         * **/
        drawFilter.setMatrix(OM);

        mAfFilter.create();

        mSlideFilterGroup.init();
    }


    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        Log.i(TAG, i + "=====onSurfaceChanged()=====" + i1);
        width = i;
        height = i1;
        //清除遗留的
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);
        /**创建一个帧染缓冲区对象*/
        GLES20.glGenFramebuffers(1, fFrame, 0);
        /**根据纹理数量 返回的纹理索引*/
        GLES20.glGenTextures(1, fTexture, 0);
       /* GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width,
                height);*/
        /**将生产的纹理名称和对应纹理进行绑定*/
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[0]);
        /**根据指定的参数 生产一个2D的纹理 调用该函数前  必须调用glBindTexture以指定要操作的纹理*/
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mPreviewWidth, mPreviewHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        useTexParameter();
        //自己的手机添加上该代码则预览不出来
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        Log.i(TAG, mPreviewWidth + "=====onSurfaceChanged()=====" + mPreviewHeight);
        mAfFilter.setSize(mPreviewWidth, mPreviewHeight);
        drawFilter.setSize(mPreviewWidth, mPreviewHeight);
        /*** 添加黑屏 ***/
        mSlideFilterGroup.onSizeChanged(mPreviewWidth, mPreviewHeight);

        MatrixUtils.getShowMatrix(SM, mPreviewWidth, mPreviewHeight, width, height);
        showFilter.setMatrix(SM);
    }

    /**
     * 切换摄像头的时候
     * 会出现画面颠倒的情况
     * 通过跳帧来解决
     */
    boolean switchCamera = false;

    public void switchCamera() {
        switchCamera = true;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {

        Log.i(TAG, "====onDrawFrame()====");
        /**更新界面中的数据*/
        mSurfaceTextrue.updateTexImage();

        EasyGlUtils.bindFrameTexture(fFrame[0], fTexture[0]);
        GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
        drawFilter.draw();
        EasyGlUtils.unBindFrameBuffer();

        if (filterIndex >= 0) {
            mSlideFilterGroup.onDrawFrame(fTexture[0], filterIndex);
            mAfFilter.setTextureId(mSlideFilterGroup.getOutputTexture());
        } else {
            //默认不使用滤镜效果 不绘制 SlideGpuFilterGroup 滤镜
            mAfFilter.setTextureId(fTexture[0]);
        }
        mAfFilter.draw();

        /**绘制显示的filter*/

        GLES20.glViewport(0, 0, width, height);
        showFilter.setTextureId(mAfFilter.getOutputTexture());
        showFilter.draw();
    }


    /**
     * 设置预览效果的size
     */
    public void setPreviewSize(int width, int height) {
        if (mPreviewWidth != width || mPreviewHeight != height) {
            mPreviewWidth = width;
            mPreviewHeight = height;
        }
    }

    /**
     * 根据摄像头设置纹理映射坐标
     */
    public void setCameraId(int id) {
        drawFilter.setFlag(id);
    }

    public SurfaceTexture getTexture() {
        return mSurfaceTextrue;
    }


    public void setFilterIndex(int index) {
        filterIndex = index;
    }

    /**
     * 创建显示的texture
     */
    private int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public void useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }
}

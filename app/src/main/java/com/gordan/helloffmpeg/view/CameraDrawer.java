package com.gordan.helloffmpeg.view;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.gordan.helloffmpeg.R;
import com.gordan.helloffmpeg.filter.AFilter;
import com.gordan.helloffmpeg.filter.CameraFilter;
import com.gordan.helloffmpeg.filter.GroupFilter;
import com.gordan.helloffmpeg.filter.NoFilter;
import com.gordan.helloffmpeg.filter.SlideGpuFilterGroup;
import com.gordan.helloffmpeg.filter.WaterMarkFilter;
import com.gordan.helloffmpeg.util.EasyGlUtils;
import com.gordan.helloffmpeg.util.MatrixUtils;
import com.gordan.helloffmpeg.video.TextureMovieEncoder;

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

    private final GroupFilter mBeFilter;
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

    private TextureMovieEncoder videoEncoder;
    private boolean recordingEnabled;
    private int recordingStatus;
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static final int RECORDING_PAUSE=3;
    private static final int RECORDING_RESUME=4;
    private static final int RECORDING_PAUSED=5;
    private String savePath;

    private int textureID;
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];

    private float[] SM = new float[16];     //用于显示的变换矩阵

    private int filterIndex = -1;

    public CameraDrawer(Context mContext) {
        //初始化一个滤镜 也可以叫控制器
        showFilter = new NoFilter(mContext.getResources());
        drawFilter = new CameraFilter(mContext.getResources());
        mBeFilter=new GroupFilter(mContext.getResources());
        mAfFilter = new GroupFilter(mContext.getResources());
        mSlideFilterGroup = new SlideGpuFilterGroup();

        //必须传入上下翻转的矩阵
        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM, false, true);//矩阵上下翻转

        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(mContext.getResources());
        waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.video_water));
        waterMarkFilter.setPosition(30,60,0,0);

        mBeFilter.addFilter(waterMarkFilter);

        recordingEnabled=false;
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

        mBeFilter.create();
        mAfFilter.create();

        mSlideFilterGroup.init();


        if (recordingEnabled){
            recordingStatus = RECORDING_RESUMED;
        } else{
            recordingStatus = RECORDING_OFF;
        }
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
        mBeFilter.setSize(mPreviewWidth,mPreviewHeight);
        mAfFilter.setSize(mPreviewWidth, mPreviewHeight);
        drawFilter.setSize(mPreviewWidth, mPreviewHeight);
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

        //在这里是先绘制的水印 再选择性绘制滤镜特效，其实还可以先绘制滤镜特效再绘制水印
        mBeFilter.setTextureId(fTexture[0]);
        mBeFilter.draw();

        if (filterIndex >= 0) {
            mSlideFilterGroup.onDrawFrame(mBeFilter.getOutputTexture(), filterIndex);
            mAfFilter.setTextureId(mSlideFilterGroup.getOutputTexture());
        } else {
            //默认不使用滤镜效果 不绘制 SlideGpuFilterGroup 滤镜
            mAfFilter.setTextureId(mBeFilter.getOutputTexture());
        }
        mAfFilter.draw();




        if (recordingEnabled){
            /**说明是录制状态*/
            switch (recordingStatus){
                case RECORDING_OFF:
                    videoEncoder = new TextureMovieEncoder();
                    videoEncoder.setPreviewSize(mPreviewWidth,mPreviewHeight);
                    videoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            savePath, mPreviewWidth, mPreviewHeight,
                            3500000, EGL14.eglGetCurrentContext(),
                            null));
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    videoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    videoEncoder.resumeRecording();
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                case RECORDING_PAUSED:
                    break;
                case RECORDING_PAUSE:
                    videoEncoder.pauseRecording();
                    recordingStatus = RECORDING_PAUSED;
                    break;

                case RECORDING_RESUME:
                    videoEncoder.resumeRecording();
                    recordingStatus=RECORDING_ON;
                    break;

                default:
                    throw new RuntimeException("unknown recording status "+recordingStatus);
            }

        }else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                case RECORDING_PAUSE:
                case RECORDING_RESUME:
                case RECORDING_PAUSED:
                    videoEncoder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown recording status " + recordingStatus);
            }
        }
        /**绘制显示的filter*/

        GLES20.glViewport(0, 0, width, height);

        showFilter.setTextureId(mAfFilter.getOutputTexture());
        showFilter.draw();

        if (videoEncoder != null && recordingEnabled && recordingStatus == RECORDING_ON){
            videoEncoder.setTextureId(mAfFilter.getOutputTexture());
            videoEncoder.frameAvailable(mSurfaceTextrue);
        }
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


    public void startRecord() {
        recordingEnabled=true;
    }

    public void stopRecord() {
        recordingEnabled=false;
    }

    public void setSavePath(String path) {
        this.savePath=path;
    }

    public void onPause(boolean auto) {
        if(auto){
            videoEncoder.pauseRecording();
            if(recordingStatus==RECORDING_ON){
                recordingStatus=RECORDING_PAUSED;
            }
            return;
        }
        if(recordingStatus==RECORDING_ON){
            recordingStatus=RECORDING_PAUSE;
        }
    }

    public void onResume(boolean auto) {
        if(auto){
            if(recordingStatus==RECORDING_PAUSED){
                recordingStatus=RECORDING_RESUME;
            }
            return;
        }
        if(recordingStatus==RECORDING_PAUSED){
            recordingStatus=RECORDING_RESUME;
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

package com.gordan.helloffmpeg.view;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.gordan.helloffmpeg.controller.CameraController;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;



public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    private Context mContext;

    private CameraDrawer mCameraDrawer;

    private CameraController mCamera;

    private int dataWidth = 0, dataHeight = 0;

    private boolean isSetParm = false;

    private int cameraId;

    public CameraView(Context context) {
        this(context, null);
        this.mContext=context;
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext=context;
        init();
    }

    private void init() {
        /**初始化OpenGL的相关信息*/
        setEGLContextClientVersion(2);//设置版本
        setRenderer(this);//设置Renderer
        setRenderMode(RENDERMODE_WHEN_DIRTY);//主动调用渲染
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);//相机距离

        /**初始化Camera的绘制类*/
        mCameraDrawer = new CameraDrawer(this.mContext);
        /**初始化相机的管理类*/
        mCamera = new CameraController();

    }

    private void open(int cameraId) {
        mCamera.close();
        mCamera.open(cameraId);
        mCameraDrawer.setCameraId(cameraId);
        final Point previewSize = mCamera.getPreviewSize();
        dataWidth = previewSize.x;
        dataHeight = previewSize.y;
        SurfaceTexture texture = mCameraDrawer.getTexture();
        texture.setOnFrameAvailableListener(this);
        mCamera.setPreviewTexture(texture);
        mCamera.preview();
    }
    /**
     * 切换前后置摄像头
     * */
    public void switchCamera() {
        cameraId = cameraId == 0 ? 1 : 0;
        mCameraDrawer.switchCamera();
        open(cameraId);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraDrawer.onSurfaceCreated(gl, config);
        if (!isSetParm) {
            open(cameraId);
            stickerInit();
        }
        mCameraDrawer.setPreviewSize(dataWidth, dataHeight);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraDrawer.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isSetParm) {
            mCameraDrawer.onDrawFrame(gl);
        }
    }

    /**
     * 每次Activity onResume时被调用,第一次不会打开相机
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isSetParm) {
            open(cameraId);
        }
    }

    public void onDestroy() {
        if (mCamera != null) {
            mCamera.close();
        }
    }

    /**
     * 摄像头聚焦
     */
    public void onFocus(Point point,int width,int height ,Camera.AutoFocusCallback callback) {
        mCamera.onFocus(point,width,height ,callback);
    }

    public int getCameraId() {
        return cameraId;
    }

    public void startRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.startRecord();
            }
        });
    }

    public void stopRecord() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.stopRecord();
            }
        });
    }

    public void setSavePath(String path) {
        mCameraDrawer.setSavePath(path);
    }

    public void resume(final boolean auto) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onResume(auto);
            }
        });
    }

    public void pause(final boolean auto) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraDrawer.onPause(auto);
            }
        });
    }


    public void takePicture(Camera.PictureCallback callback)
    {
        mCamera.takePicture(callback);
    }

    public void onTouch(final MotionEvent event) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                //mCameraDrawer.onTouch(event);
            }
        });
    }

    private void stickerInit() {
        if (!isSetParm && dataWidth > 0 && dataHeight > 0) {
            isSetParm = true;
        }
    }

    public void switchFilterIndex(int index)
    {
        mCameraDrawer.setFilterIndex(index);
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }


}

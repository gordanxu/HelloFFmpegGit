package com.gordan.helloffmpeg;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.PopupWindow;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.adapter.MusicAdapter;
import com.gordan.helloffmpeg.model.MusicModel;
import com.gordan.helloffmpeg.util.Constant;
import com.gordan.helloffmpeg.util.FfmpegUtil;
import com.gordan.helloffmpeg.util.FileUtil;
import com.gordan.helloffmpeg.view.AutoFitTextureView;
import com.gordan.helloffmpeg.view.CustomHorizontalProgress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.OnClick;

/********
 * 视频拍摄时间默认是 15 秒 若中途用户主动放弃 则程序记录拍摄时间
 *
 *
 *
 *
 *
 *
 *
 * *********/


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends BaseActivity implements MusicAdapter.ItemClickInterface
{
    final static String TAG = CameraActivity.class.getSimpleName();

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    CameraDevice mCameraDevice;

    HandlerThread mWorkThread;

    Handler mWorkHandler;

    @Bind(R.id.tv_video)
    AutoFitTextureView mTextureView;

    @Bind(R.id.pb_video)
    CustomHorizontalProgress mProgressBarVideo;

    int mProgress=0;

    CameraCaptureSession mCaptureSession;

    ImageReader mImageReader;

    MediaRecorder mMediaRecorder;

    File sdcardFile;

    private Size mVideoSize;

    private Size mPreviewSize;

    private Integer mSensorOrientation;

    CaptureRequest.Builder mPreviewBuilder;

    String cameraId = CameraCharacteristics.LENS_FACING_FRONT + "";

    ExecutorService mExecutorService;

    //信号量 相当于同步代码块（锁）
    private Semaphore mCameraLock = new Semaphore(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        Log.i(TAG,mDisplayMetrics.widthPixels+"======mDisplayMetrics======"+mDisplayMetrics.heightPixels);
        sdcardFile = Environment.getExternalStorageDirectory();

        mExecutorService=Executors.newFixedThreadPool(2);

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {

                FileUtil.copyFileFromAssets(CameraActivity.this,"music",sdcardFile.getAbsolutePath());

            }
        });

    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_camera;
    }

    @Override
    protected void handleBaseMessage(Message message) {

        switch (message.what) {

            case Constant.MSG_VIDEO_PROGRESS:

                Log.i(TAG,"======mProgress======"+mProgress);
                if(mProgress>=Constant.VIDEO_TAKE_MAX_TIME)
                {
                    mHandler.removeMessages(Constant.MSG_VIDEO_PROGRESS);

                    stopRecordingVideo();

                    break;
                }
                //刘琦遇到过同样的问题 直接连续除的话 per 为 0
                mProgress+=1000;
                double per=(double)mProgress/Constant.VIDEO_TAKE_MAX_TIME;
                Log.i(TAG,"======per======"+per);
                int value=(int)(per*100);
                Log.i(TAG,"======value======"+value);
                mProgressBarVideo.setProgress(value);
                mHandler.sendEmptyMessageDelayed(Constant.MSG_VIDEO_PROGRESS,1000);

                break;

            case Constant.MSG_VIDEO_MAIN_THREAD:

                mIsRecordingVideo = true;
                mMediaRecorder.start();

                break;

            case Constant.MSG_TAKE_VIDEO:



                break;

                case Constant.MSG_VIDEO_MUSIC_FINISH:

                    if(mProgressDialog!=null)
                    {
                        mProgressDialog.dismiss();
                        mProgressDialog=null;
                    }

                    String url=sdcardFile.getAbsolutePath()+File.separator+"output.mp4";

                    Intent intent=new Intent(this,PlayerActivity.class);
                    intent.putExtra("url",url);
                    this.startActivity(intent);

                    break;
        }

    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (DEFAULT_ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "====onSurfaceTextureAvailable()======");
            openCamera(width, height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "====onSurfaceTextureSizeChanged()======");
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.i(TAG, "====onSurfaceTextureDestroyed()======");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //事件不断往上抛
            //Log.i(TAG,"====onSurfaceTextureUpdated()======");

        }
    };


    CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "====onOpened()======");
            mCameraDevice = camera;

            //开启预览
            startPreview();

            mCameraLock.release();

            if (null != mTextureView) {

                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.i(TAG, "====onDisconnected()======");
            mCameraLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.i(TAG, "====onError()======");
            mCameraLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };


    ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "====onImageAvailable()======");
            //这里的回调不会每次都执行 只会在拍照结束以后调用
            File imageFile = new File(sdcardFile.getAbsolutePath(), "gordan.jpg");

            mWorkHandler.post(new ImageSaver(reader.acquireNextImage(), imageFile));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "====onResume()======");
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mTextureListener);
        }

    }


    private void startBackgroundThread() {
        mWorkThread = new HandlerThread("camera2");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper());
    }

    private void startPreview() {

        //关闭上一次的预览会话
        closePreviewSession();

        SurfaceTexture mTexture = mTextureView.getSurfaceTexture();

        assert mTexture != null;

        try {

            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface mPreSurface = new Surface(mTexture);
            mPreviewBuilder.addTarget(mPreSurface);

            mTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getWidth());
            //Collections.singletonList(mPreSurface) 照片的输出目的地添加上 ImageReader 要不然图片无法保存
            mCameraDevice.createCaptureSession(Arrays.asList(mPreSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Log.i(TAG, "=====onConfigured()======");
                            mCaptureSession = session;
                            //更新画面
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.i(TAG, "=====onConfigureFailed()======");

                        }
                    }, mWorkHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        try {
            //自动对焦
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //这里的Thread不就是冗余代码嘛？
            /*HandlerThread thread = new HandlerThread("Preview");
            thread.start();*/
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mWorkHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void openCamera(int width, int height) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        Log.i(TAG,width+"======openCamera======="+height);
        try {

            CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.i(TAG, "=====Camera Time out!=====");
                return;
            }

            String[] cameraIds = mCameraManager.getCameraIdList();

            for (String id : cameraIds) {
                Log.i(TAG, "=====cameraId=====" + id);
            }

            //后置摄像头
            //cameraId = CameraCharacteristics.LENS_FACING_FRONT+"";

            //获取相机的属性
            CameraCharacteristics mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //获取相机的角度
            mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            //获取视频输出的最佳尺寸
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

            //获取相机的最佳预览尺寸
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);

            mImageReader = ImageReader.newInstance(mVideoSize.getWidth(), mVideoSize.getHeight(), ImageFormat.JPEG, 2);

            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mWorkHandler);

            //获取手机屏幕方向
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //如果是横屏
                //mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            } else {
                //mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            configureTransform(width, height);

            mMediaRecorder = new MediaRecorder();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                //打开相机
                mCameraManager.openCamera(cameraId, mStateCallback, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void startVideoRecording() {
        try {
            //关闭上一次的预览会话
            closePreviewSession();

            //设置音视频的相关格式
            setUpMediaRecorder();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            //视频输出的目标纹理（布局纹理 和 录音机的纹理） 预览的时候是布局的纹理和ImageReader的纹理
            List<Surface> mSurfaces = new ArrayList<>();

            Surface mSurface = new Surface(texture);
            mSurfaces.add(mSurface);
            mPreviewBuilder.addTarget(mSurface);

            Surface videoSurface = mMediaRecorder.getSurface();
            mSurfaces.add(videoSurface);
            mPreviewBuilder.addTarget(videoSurface);

            mCameraDevice.createCaptureSession(mSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {

                    Log.i(TAG, "======onConfigured()=======");
                    //刷新预览会话
                    mCaptureSession = session;

                    updatePreview();

                    mHandler.sendEmptyMessage(Constant.MSG_VIDEO_MAIN_THREAD);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.i(TAG, "======onConfigureFailed()=======");

                }
            }, mWorkHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpMediaRecorder() {
        try {

            File mOutPutFile = new File(Environment.getExternalStorageDirectory(), "gordan.mp4");

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(mOutPutFile.getAbsolutePath());

            //设置视频的 码率 kbps
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            //设置视频的 帧率 fps
            mMediaRecorder.setVideoFrameRate(30);
            //设置视频画面的尺寸
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            //设置视频的编码格式
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //设置音频的编码格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            //设置相机的旋转角度（若不设置 相机则是横向的）
            int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
            switch (mSensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }

            mMediaRecorder.prepare();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean mIsRecordingVideo;

    private void stopRecordingVideo() {

        //选择要合成的音乐
        initPopWindows();

        // UI
        mIsRecordingVideo = false;
        mProgress=0;
        mProgressBarVideo.setProgress(0);
        mHandler.removeMessages(Constant.MSG_VIDEO_PROGRESS);
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        //摄像的铃声找不到--
        shootSound("file:///system/media/audio/ui/camera_click.ogg");
        //再次开启预览
        startPreview();
    }

    MediaPlayer mMediaPlayer = null;

    /***
     * 播放拍照的系统提示音
     * ***/
    private void shootSound(String url) {

        try {
            if(mMediaPlayer!=null)
            {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(this, Uri.parse(url));
            }
            else
            {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                mMediaPlayer.setDataSource(this, Uri.parse(url));
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {

                        mMediaPlayer.start();
                    }
                });
            }
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        try {
            CaptureRequest.Builder mBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mBuilder.addTarget(mImageReader.getSurface());
            mBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

            CameraCaptureSession.CaptureCallback tempCallBack = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                    Log.i(TAG, "====2======onCaptureStarted()");
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                    super.onCaptureProgressed(session, request, partialResult);

                    Log.i(TAG, "====2======onCaptureProgressed()");
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.i(TAG, "====2======onCaptureCompleted()");
                    shootSound("file:///system/media/audio/ui/camera_click.ogg");
                    //再次开启预览
                    startPreview();
                }
            };

            // Orientation
            int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
            mBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                //Android8.0+的设备上执行代码   应用直接崩溃
                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
            }
            mCaptureSession.capture(mBuilder.build(), tempCallBack, null);

            //mCameraCaptureSession.setRepeatingRequest(mBuilder.build(),tempCallBack,mCameraHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnClick({R.id.iv_switch, R.id.iv_photo, R.id.iv_video})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.iv_switch:

                if ("0".equalsIgnoreCase(cameraId)) {
                    cameraId = CameraCharacteristics.LENS_FACING_BACK + "";
                } else {
                    cameraId = CameraCharacteristics.LENS_FACING_FRONT + "";
                }

                closeCamera();

                openCamera(mTextureView.getWidth(), mTextureView.getHeight());

                break;

            case R.id.iv_photo:

                takePicture();

                break;

            case R.id.iv_video:

                if (mIsRecordingVideo) {
                    if (mHandler.hasMessages(Constant.MSG_VIDEO_PROGRESS)) {
                        mHandler.removeMessages(Constant.MSG_VIDEO_PROGRESS);
                    }
                    stopRecordingVideo();
                } else {
                    showText("再按一次完成拍摄！");
                    mProgress=0;
                    mHandler.sendEmptyMessage(Constant.MSG_VIDEO_PROGRESS);
                    startVideoRecording();
                }
                break;
        }
    }

    List<MusicModel> mList=null;

    PopupWindow mMusicPopupWindow;

    private void initPopWindows()
    {
        mList=new ArrayList<>();
        try {
            String[] fileNames=this.getAssets().list("music");

            int i=0;

            for (String fileName:fileNames)
            {
                MusicModel model=new MusicModel();
                model.id=i;
                model.name=fileName;
                if(fileName.endsWith("png"))
                {
                    Log.i(TAG,"====picture file so continue==");
                    continue;
                }

                mList.add(model);
                i++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        MusicAdapter mMusicAdapter;

        View popContentView= LayoutInflater.from(this).inflate(R.layout.pop_music,null);

        RecyclerView musicRecycleView=(RecyclerView) popContentView.findViewById(R.id.rv_music);
        musicRecycleView.setLayoutManager(new LinearLayoutManager(this));

        mMusicAdapter=new MusicAdapter(this,mList,R.layout.item_music);
        mMusicAdapter.setItemClickListener(this);
        musicRecycleView.setAdapter(mMusicAdapter);

        int width=mDisplayMetrics.widthPixels*4/5;
        int height=mDisplayMetrics.heightPixels/3;

        mMusicPopupWindow=new PopupWindow(popContentView,width,height,true);
        mMusicPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mMusicPopupWindow.setFocusable(true);
        mMusicPopupWindow.setOutsideTouchable(true);
        mMusicPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {

                //如果没有选择音乐 则直接播放界面
                if(!mHandler.hasMessages(Constant.MSG_VIDEO_MUSIC_FINISH))
                {
                    //mHandler.sendEmptyMessage(Constant.MSG_VIDEO_MUSIC_FINISH);
                }

                releasePlayer();

                mMusicPopupWindow=null;
            }
        });

        mMusicPopupWindow.showAtLocation(mTextureView,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,0);
    }

    private void closeCamera() {
        try {
            mCameraLock.tryAcquire();

            closePreviewSession();

            if (mCameraDevice != null) {
                mCameraDevice.close();
            }

            if (mMediaRecorder != null) {
                mMediaRecorder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mCameraLock.release();
        }
    }

    private void stopBackgroundThread() {
        mWorkThread.quitSafely();
        try {
            mWorkThread.join();
            mWorkThread = null;
            mWorkHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {

        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }



    @Override
    public void onItemClick(View item, int position) {
        Log.i(TAG,"=====onItemClick======"+position);
        if(position<mList.size())
        {
            MusicModel model=mList.get(position);
            String url=sdcardFile.getAbsolutePath()+File.separator+model.name;
            Log.i(TAG,"======url====="+url);

            shootSound(url);
        }
    }

    MaterialDialog mProgressDialog;

    @Override
    public void onItemUseClick(View item, int position) {
        Log.i(TAG,"=====onItemUseClick======"+position);
        if(position<mList.size())
        {
            MusicModel model=mList.get(position);
            String url=sdcardFile.getAbsolutePath()+File.separator+model.name;
            Log.i(TAG,"======url====="+url);

            //提前发送 （用户可能不选择铃声）
            //mHandler.sendEmptyMessageDelayed(Constant.MSG_VIDEO_MUSIC_FINISH,30000);

            mMusicPopupWindow.dismiss();

            //第一个参数为true 圆形进度条 否则为水平
            //如何检测消失？
            MaterialDialog.Builder mBuilder=new MaterialDialog.Builder(this);
            mProgressDialog=mBuilder.content("视频生成中,请稍后...").progress(true,100,true)
                    .progressNumberFormat("%1d/%2d").canceledOnTouchOutside(false).build();
            mProgressDialog.show();

            String jniStr="ffmpeg -i "+sdcardFile.getAbsolutePath()+File.separator+"gordan.mp4 -i "+
                    url+" -c:v copy -c:a mp3 -acodec libmp3lame -t 15 -strict experimental -map 0:v:0 -map 1:a:0 "+
                    sdcardFile.getAbsolutePath()+File.separator+"output.mp4";
            Log.i(TAG,"======jniStr====="+jniStr);
            //开始合成音乐与视频
            executeConvertCommand(jniStr);
        }
    }

    FfmpegUtil mFfmpegUtil;

    private synchronized void executeConvertCommand(String command)
    {
        if(mFfmpegUtil==null)
        {
            mFfmpegUtil=new FfmpegUtil();
        }

        mExecutorService.execute(new Runnable() {

            @Override
            public void run()
            {
                Log.i(TAG,"=====ready======");
                String[] cmd=command.split(" ");
                int result= mFfmpegUtil.convertVideoFormat(cmd);
                Log.i(TAG,"=====result======"+result);
                mHandler.sendEmptyMessage(Constant.MSG_VIDEO_MUSIC_FINISH);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "=====onPause()======");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "=====onStop()======");
        stopBackgroundThread();
        closeCamera();
    }


    private void releasePlayer()
    {
        if(mMediaPlayer!=null)
        {
            if(mMediaPlayer.isPlaying())
            {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer=null;
        }
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "=====onDestroy()======");
        releasePlayer();
        super.onDestroy();
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {

           /* if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }*/
            //希望视频的输出尺寸和预览尺寸一样大 获取的尺寸宽高恰好是反的
            if(size.getHeight() == mDisplayMetrics.widthPixels)
            {
                Log.i(TAG,size.getWidth()+"====chooseVideoSize====="+size.getHeight());
                return size;
            }

        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {

            Log.i(TAG, "=====ImageSaver=======run()=====");
            //获取相机的一帧数据
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Log.i(TAG, "=====save image success=====");
            }
        }

    }

}

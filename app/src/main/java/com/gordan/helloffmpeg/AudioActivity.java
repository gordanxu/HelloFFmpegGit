package com.gordan.helloffmpeg;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.util.PcmToWavUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.Bind;
import butterknife.OnClick;

public class AudioActivity extends BaseActivity {

    final static String TAG = AudioActivity.class.getSimpleName();

    @Bind(R.id.et_audio_output)
    EditText etAudioPath;

    @Bind(R.id.btn_audio)
    Button btnAudio;

    private AudioRecord mAudioRecord;

    private int readBufSize = 0;

    byte[] data = null;

    private int sampleRateInHz = 4410,//采样率

    channelConfig = AudioFormat.CHANNEL_IN_MONO,//声道数(单声道)

    audioFormat = AudioFormat.ENCODING_PCM_16BIT;//音频格式

    boolean isRecording;

    File sdcardFile;

    String outputPath = "gordan.wav";//添加包头之后文件名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        sdcardFile = Environment.getExternalStorageDirectory();
    }


    @Override
    protected int inflateResId() {
        return R.layout.activity_audio;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @OnClick({R.id.btn_audio, R.id.btn_convert, R.id.btn_play})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_audio:

                String path = etAudioPath.getText() + "";
                path = path.trim();
                try {
                    if (btnAudio.getTag() != null && "1".equalsIgnoreCase(btnAudio.getTag() + "")) {
                        //再按一次停止录音
                        Log.i(TAG, "======stop()=====");
                        isRecording = false;
                        mAudioRecord.stop();

                        btnAudio.setText("开始录音");
                        btnAudio.setTag("0");

                    } else {

                        //按一次 开始录音
                        if (mAudioRecord == null) {
                            readBufSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
                            data = new byte[readBufSize];
                            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, readBufSize);
                            final File pcmFile = new File(sdcardFile, path);
                            if (pcmFile.exists()) {
                                pcmFile.delete();
                            }

                            Log.i(TAG, "========pcmFile:" + pcmFile.getAbsolutePath());
                            //开始录音
                            mAudioRecord.startRecording();
                            isRecording = true;
                            Log.i(TAG, "======start()=====");
                            //开启工作线程 将硬件中采集到声音数据 写入文件中
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        FileOutputStream fos = new FileOutputStream(pcmFile);

                                        if (fos != null) {
                                            while (isRecording) {
                                                int read = mAudioRecord.read(data, 0, readBufSize);
                                                if (AudioRecord.ERROR_BAD_VALUE != read) {
                                                    fos.write(data);
                                                }
                                            }
                                        }

                                        fos.flush();
                                        fos.close();
                                    } catch (IOException eio) {
                                        eio.printStackTrace();
                                    }
                                }
                            }).start();
                        } else {
                            mAudioRecord.startRecording();
                            isRecording = true;
                        }

                        btnAudio.setText("录音中...");
                        btnAudio.setTag("1");

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;

            case R.id.btn_convert:
                path = etAudioPath.getText() + "";
                path = path.trim();
                PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(sampleRateInHz, channelConfig, audioFormat);

                File pcmFile = new File(sdcardFile, path);
                File wavFile = new File(sdcardFile, outputPath);

                if (wavFile.exists()) {
                    wavFile.delete();
                }
                pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());

                showText("转码完成");

                break;

            case R.id.btn_play:

                //利用AudioTrack播放裸流
                path = etAudioPath.getText() + "";
                path = path.trim();

                playInModeStream(path);

                break;
        }

    }



    FileInputStream fileInputStream;

    AudioTrack audioTrack;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void playInModeStream(String name) {
        /*
         * SAMPLE_RATE_INHZ 对应pcm音频的采样率
         * channelConfig 对应pcm音频的声道
         * AUDIO_FORMAT 对应pcm音频的格式
         * */
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        final int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder().setSampleRate(sampleRateInHz)
                        .setEncoding(audioFormat)
                        .setChannelMask(channelConfig)
                        .build(),
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        audioTrack.play();

        Log.i(TAG, "========start play=======");
        File file = new File(sdcardFile, name);
        try {
            fileInputStream = new FileInputStream(file);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] tempBuffer = new byte[minBufferSize];
                        while (fileInputStream.available() > 0) {
                            int readCount = fileInputStream.read(tempBuffer);
                            if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                                    readCount == AudioTrack.ERROR_BAD_VALUE) {
                                continue;
                            }
                            if (readCount != 0 && readCount != -1) {
                                audioTrack.write(tempBuffer, 0, readCount);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

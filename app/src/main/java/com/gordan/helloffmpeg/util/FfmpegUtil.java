package com.gordan.helloffmpeg.util;

public class FfmpegUtil {


    static {
        System.loadLibrary("native-lib");
    }

    public native String cpuInfo();

    public native String urlprotocolinfo();

    public native String avformatinfo();

    public native String avcodecinfo();

    public native String avfilterinfo();

    public native String configurationinfo();

    public native int decode(String inputurl, String outputurl);

    public native int convertVideoFormat(String[] cmdLine);

    public native void mergeVideoAndAudio(String inputVideo,String inputAudio,String outputMedia);

    public native void separateVideoAndAudio(String inputTs,String outputVideo,String outputAudio);

    public native void convertMediaFormat(String input,String output);


}

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

}
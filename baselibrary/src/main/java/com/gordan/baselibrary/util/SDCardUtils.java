package com.gordan.baselibrary.util;

import android.os.Environment;

import java.io.File;

/**
 * 功能描述:SD卡相关配置<br>
 * 创建者:wangyang<br>
 * 创建时间：2017/4/7<br>
 * 修改时间:<br>
 */
public class SDCardUtils {

    private SDCardUtils() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * 判断SDCard是否可用
     *
     * @return
     */
    public static boolean isSDCardEnable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取SD卡路径
     *
     * @return
     */
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

    /**
     * SD卡的Internal位置
     *
     * @return
     */
    public static String getSDPathInternal() {
        return "mnt/internal_sd/";
    }

    /**
     * 获取系统存储路径
     *
     * @return
     */
    public static String getRootDirectoryPath() {
        return Environment.getRootDirectory().getAbsolutePath();
    }
}

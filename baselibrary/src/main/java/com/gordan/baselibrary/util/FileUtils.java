package com.gordan.baselibrary.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    final static String TAG=FileUtils.class.getSimpleName();

    final static String FILE_NAME = "channel.txt";

    /****
     * 将内存中的字符串保存到文件（文件名已经在类中预设值）
     *
     * @param content 需要保存的字符串
     * @param path 保存的路径
     */
    public static boolean saveStringToFile(String content, String path)  {

        byte[] buf = null;

        FileOutputStream fos = null;
        // 储存下载文件的目录
        try {
            LogUtils.i(TAG,"==saveStringToFile==",false);
            buf = content.getBytes();
            File file = new File(path, FILE_NAME);
            if(!file.exists())
            {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            fos.write(buf, 0, buf.length);
            fos.flush();
            LogUtils.i(TAG,"==saveStringToFile===success=",false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG,"==saveStringToFile==false",false);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /***
     * 从文件中获取字符串（上一方法的逆过程）
     *
     * @param path 文件的路径
     * @return
     */
    public static String getStringFromFile(String path) {
        String content = "";
        FileInputStream ios = null;
        try {
            LogUtils.i(TAG,"==getStringFromFile==",false);
            File file = new File(path, FILE_NAME);
            ios = new FileInputStream(file);
            byte[] bytes = new byte[ios.available()];
            ios.read(bytes);
            content=new String(bytes,"UTF-8");
            LogUtils.i(TAG,"==getStringFromFile==success",false);
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.i(TAG,"==getStringFromFile==failed",false);
        } finally {
            try {
                if (ios != null) {
                    ios.close();
                }

            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }

        return content;
    }






}

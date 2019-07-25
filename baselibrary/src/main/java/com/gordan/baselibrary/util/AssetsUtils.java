package com.gordan.baselibrary.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetsUtils {

    public static Bitmap getImageFromAssets(Context mContext, String fileName) {
        Bitmap image = null;
        AssetManager am = mContext.getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return image;
        }
    }

    public static void copyFileFromAssets(Context mContext, String assetsPath, String savePath) {
        try {
            String filename = assetsPath.substring(assetsPath.lastIndexOf("/") + 1);
            InputStream is = mContext.getAssets().open(assetsPath);
            File destFile = new File(savePath, filename);
            if (destFile.exists()) {
                return;
            }

            FileOutputStream fos = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                // buffer字节
                fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
            }
            fos.flush();// 刷新缓冲区
            is.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /***
     *
     * 拷贝Assets目录下面的子目录/文件
     *
     * @param context
     * @param srcPath 拷贝整个Assets目录 可传 空字符
     * @param dstPath
     */
    public static void copyAssetsToSDCard(Context context, String srcPath, String dstPath) {
        try {
            String fileNames[] = context.getAssets().list(srcPath);
            if (fileNames.length > 0) {
                //如果式目录
                File file = new File(Environment.getExternalStorageDirectory(), dstPath);
                if (!file.exists()) file.mkdirs();
                for (String fileName : fileNames) {
                    if (!srcPath.equals("")) { // assets 文件夹下的子目录 递归调用
                        copyAssetsToSDCard(context, srcPath + File.separator + fileName, dstPath + File.separator + fileName);
                    } else { // assets文件夹下的所有子目录和文件（此时 srcPath 参数为空字符） 递归调用
                        copyAssetsToSDCard(context, fileName, dstPath + File.separator + fileName);
                    }
                }
            } else {
                //如果是文件
                File outFile = new File(Environment.getExternalStorageDirectory(), dstPath);
                InputStream is = context.getAssets().open(srcPath);
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();


        }
    }


}

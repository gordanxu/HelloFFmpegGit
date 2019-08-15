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

    final static String TAG=AssetsUtils.class.getSimpleName();

    /******
     * 从Assets目录拷贝图片
     *
     * @param mContext
     * @param fileName 图片名称
     * @return Bitmap对象
     */
    public static Bitmap getImageFromAssets(Context mContext, String fileName) {
        Bitmap image = null;
        AssetManager am = mContext.getAssets();
        try {
            LogUtils.i(TAG,"====getImageFromAssets======",false);
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return image;
        }
    }

    /*****
     * 拷贝Assets目录指定文件到指定路径
     *
     * @param mContext
     * @param assetsPath
     * @param savePath
     * @return
     */
    public static boolean copyFileFromAssets(Context mContext, String assetsPath, String savePath) {
        try {
            LogUtils.i(TAG,"=====copyFileFromAssets=====",false);
            String filename = assetsPath.substring(assetsPath.lastIndexOf("/") + 1);
            InputStream is = mContext.getAssets().open(assetsPath);
            File destFile = new File(savePath, filename);
            if (destFile.exists()) {
                //这里的业务逻辑 多半是拷贝过 所以就不必再次拷贝了 所以直接返回
                LogUtils.i(TAG,"===file exists so return====",false);
                return false;
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
            LogUtils.i(TAG,"=====copyFileFromAssets===success==",false);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG,"=====copyFileFromAssets==failed===",false);
            return false;
        }
        return true;
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
            LogUtils.i(TAG,"=====copyAssetsToSDCard====",false);
            String fileNames[] = context.getAssets().list(srcPath);
            if (fileNames.length > 0) {
                //如果是目录
                LogUtils.i(TAG,"=====copyDir====",false);
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
                LogUtils.i(TAG,"=====copyFile====",false);
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

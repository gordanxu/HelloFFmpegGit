package com.gordan.helloffmpeg.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    final static String TAG = FileUtil.class.getSimpleName();

    public static void copyFileFromAssets(Context context, String sourceDir, String destDir) {
        try {
            String[] fileNames = context.getAssets().list(sourceDir);

            for (String fileName : fileNames) {
                Log.i(TAG, "=====filename===" + fileName);
                InputStream is = context.getAssets().open(sourceDir + File.separator + fileName);
                File destFile = new File(destDir, fileName);
                if (destFile.exists()) {
                    Log.i(TAG, "=====file exists return=====");
                    continue;
                }

                FileOutputStream fos = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();// 刷新缓冲区
                is.close();
                fos.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}

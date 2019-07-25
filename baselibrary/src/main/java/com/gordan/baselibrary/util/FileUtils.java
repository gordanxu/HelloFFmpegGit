package com.gordan.baselibrary.util;





import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;




/**
 * @author zhangpeng
 * @parm 功能：文件操作辅助类
 * @date 创建时间：2017-11-09
 * @parm 修订记录：
 */

public class FileUtils {


    final static String CHANNEL_INFO_NAME = "channel.txt";



    public static void saveChannelInfoToFile(String content, String path) throws IOException {

        byte[] buf = null;

        FileOutputStream fos = null;
        // 储存下载文件的目录
        try {
            buf = content.getBytes();
            File file = new File(path, CHANNEL_INFO_NAME);
            if(!file.exists())
            {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            fos.write(buf, 0, buf.length);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getChannelInfoFromFile(String path) throws IOException {
        String content = "";
        FileInputStream ios = null;
        try {

            File file = new File(path, CHANNEL_INFO_NAME);
            ios = new FileInputStream(file);
            byte[] bytes = new byte[ios.available()];
            ios.read(bytes);
            content=new String(bytes,"UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
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


    /**
     * @param saveDir
     * @return
     * @throws IOException 判断下载目录是否存在
     */
    private static String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        return saveDir;
    }



}

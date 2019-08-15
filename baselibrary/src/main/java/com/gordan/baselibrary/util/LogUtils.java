package com.gordan.baselibrary.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.text.SimpleDateFormat;
import java.util.Date;

/***
 * 文本日志超出Logcat的打印长度如何处理
 *  Android系统的单条日志打印长度是有限的，长度是固定的4*1024个字符长度
 *
 *  日志的打印还得设置打印开关 可以将基础类库中的日志屏蔽
 *
 * ***/

public class LogUtils {

    private static String TAG = "LogUtils";
    private static boolean LOG_FLAG = true;
    private static int LOG_MAX_LENGTH = 2 * 1024;
    private static String logPath = null;//log日志存放路径
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");//日期格式;
    private final static long defaultSize = 10 * 1024 * 1024;   //默认10M
    private static Date date = new Date();//因为log日志是使用日期命名的，使用静态成员变量主要是为了在整个程序运行期间只存在一个.txt文件中;

    private static final char VERBOSE = 'v';

    private static final char DEBUG = 'd';

    private static final char INFO = 'i';

    private static final char WARN = 'w';

    private static final char ERROR = 'e';

    public static void init(Context context, boolean flag) {
        logPath = getFilePath(context);//获得文件储存路径,在后面加"/Logs"建立子文件夹
        //如果父路径不存在
        File file = new File(logPath);
        if (!file.exists()) {
            file.mkdirs();//创建父路径
        }

        //判断log文件大小,
        File myFile = new File(logPath);
        File[] files = myFile.listFiles();
        long size = 0;
        for (int i = 0; i < files.length; i++) {
            size += files[i].length();
        }
        if (size >= defaultSize || files.length >= 50) {
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        }

        LOG_FLAG = flag;
    }

    public static void setLogFlag(boolean logFlag) {
        LOG_FLAG = logFlag;
    }

    /**
     * 获得文件存储路径
     *
     * @return
     */
    private static String getFilePath(Context context) {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {
            //如果外部储存可用
            return context.getExternalFilesDir("Log").getPath();
        } else {
            return context.getFilesDir().getPath();//直接存在/data/data里，非root手机是看不到的
        }
    }

    public static void v(String message) {
        v(message, false);
    }

    public static void v(String message, boolean isWriteToFile) {
        v(null, message, isWriteToFile);
    }

    public static void v(String target, String message, boolean isWriteToFile) {
        if (!LOG_FLAG) {
            return;
        }
        String msg = message == null ? "null" : message;
        if (msg.length() >= LOG_MAX_LENGTH) {
            more(target, msg);
        } else {
            Log.v(target, msg);
        }
        if (isWriteToFile) {
            writeToFile(VERBOSE, target, msg);
        }
    }

    public static void d(String message) {
        d(message, false);
    }

    public static void d(String message, boolean isWriteToFile) {
        d(null, message, isWriteToFile);
    }

    public static void d(String target, String message, boolean isWriteToFile) {

        if (!LOG_FLAG) {
            return;
        }

        String msg = message == null ? "null" : message;
        if (msg.length() >= LOG_MAX_LENGTH) {
            more(target, msg);
        } else {
            Log.d(target, msg);
        }
        if (isWriteToFile) {
            writeToFile(DEBUG, target, msg);
        }
    }

    public static void i(String message) {
        i(message, false);
    }

    public static void i(String message, boolean isWriteToFile) {
        i(null, message, isWriteToFile);
    }

    public static void i(String target, String message, boolean isWriteToFile) {
        if (!LOG_FLAG) {
            return;
        }
        String msg = message == null ? "null" : message;
        if (msg.length() >= LOG_MAX_LENGTH) {
            more(target, msg);
        } else {
            Log.i(target, msg);
        }
        if (isWriteToFile) {
            writeToFile(INFO, target, msg);
        }
    }

    public static void w(String message) {
        w(message, false);
    }

    public static void w(String message, boolean isWriteToFile) {
        w(null, message, isWriteToFile);
    }

    public static void w(String target, String message, boolean isWriteToFile) {
        if (!LOG_FLAG) {
            return;
        }

        String msg = message == null ? "null" : message;
        if (msg.length() >= LOG_MAX_LENGTH) {
            more(target, msg);
        } else {
            Log.w(target, msg);
        }
        if (isWriteToFile) {
            writeToFile(WARN, target, msg);
        }
    }

    public static void e(String message) {
        e(null, message, false);
    }

    public static void e(String message, boolean isWriteToFile) {
        e(null, message, isWriteToFile);
    }

    public static void e(String target, String message, boolean isWriteToFile) {
        if (!LOG_FLAG) {
            return;
        }
        String msg = message == null ? "null" : message;
        if (message.length() >= LOG_MAX_LENGTH) {
            //日志超出则分段打印
            more(target, msg);
        } else {
            Log.e(target, msg);
        }
        if (isWriteToFile) {
            writeToFile(ERROR, target, msg);
        }
    }

    public static void more(String target, String msg) {
        int strLength = msg.length();
        int start = 0;
        int end = LOG_MAX_LENGTH;
        for (int i = 0; i < 100; i++) {
            //剩下的文本还是大于规定长度则继续重复截取并输出
            if (strLength > end) {
                Log.e(target + "_" + i, msg.substring(start, end));
                start = end;
                end = end + LOG_MAX_LENGTH;
            } else {
                Log.e(target + "_end", msg.substring(start, strLength));
                break;
            }
        }
    }

    /**
     * 将log信息写入文件中
     *
     * @param type
     * @param tag
     * @param msg
     */
    private synchronized static void writeToFile(char type, String tag, String msg) {

        if (null == logPath) {
            Log.e(TAG, "logPath == null ，未初始化LogUtils");
            return;
        }

        String fileName = logPath + "/Log_" + dateFormat.format(new Date()) + ".txt";//log日志名，使用时间命名，保证不重复
        Log.d(TAG, "fileNmae:" + fileName);
        String log = dateFormat.format(date) + "   " + type + "   " + tag + "   " + msg + "\r\n";//log日志内容，可以自行定制

        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {
            fos = new FileOutputStream(fileName, true);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(log);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

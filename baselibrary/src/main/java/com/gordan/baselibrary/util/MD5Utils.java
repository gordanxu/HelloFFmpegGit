package com.gordan.baselibrary.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密算法
 *
 *
 */
public class MD5Utils {

    final static String TAG = MD5Utils.class.getSimpleName();

    private MessageDigest md;

    private static MD5Utils md5;

    private MD5Utils() {
        try {
            LogUtils.i(TAG, "===MD5Utils===", false);
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // 产生一个MD5实例
    public static MD5Utils getInstance() {
        if (md5 == null) {
            synchronized (MD5Utils.class) {
                md5 = new MD5Utils();
            }
        }
        return md5;
    }

    /*****
     *
     * 将字符串进行 32位的 MD5 加密 返回加密后的大写字符串
     *
     * MD5 加密以后无法解密 只能比较加密后的字符串才能确认当前输入的字符和原字符串是否相同
     *
     * @param pass
     * @return
     */
    public String createMD5(String pass) {
//        md.update(toUnicodeMC(pass));
        md.update(pass.getBytes());
        byte[] b = md.digest();
        return byteToHexString(b).toUpperCase();
    }

    private String byteToHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        String temp = "";
        for (int i = 0; i < b.length; i++) {
            temp = Integer.toHexString(b[i] & 0Xff);
            if (temp.length() == 1)
                temp = "0" + temp;
            sb.append(temp);
        }
        return sb.toString();
    }

    public byte[] toUnicodeMC(String s) {
        byte[] bytes = new byte[s.length() * 2];
        for (int i = 0; i < s.length(); i++) {
            int code = s.charAt(i) & 0xffff;
            bytes[i * 2] = (byte) (code & 0x00ff);
            bytes[i * 2 + 1] = (byte) (code >> 8);
        }
        return bytes;
    }
}

package com.gordan.baselibrary.util;

/***
 *
 * 常用的正则表达式：
 *
 * https://blog.csdn.net/zpz2411232428/article/details/83549502
 *
 *
 * 其它有用的代码片段：https://github.com/Blankj/AndroidUtilCode/blob/master/lib/utilcode/README-CN.md
 *
 */
public class RegUtils {

    final static String MOBILE = "^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(17[013678])|(18[0,5-9]))\\d{8}$";

    final static String PHONE = "^(\\(\\d{3,4}-)|\\d{3,4}-\\)?\\d{7,8}$";

    final static String EMAIL = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    final static String IDENTITY_NUMBER_15 = "^[1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}$";

    final static String IDENTITY_NUMBER_15_18 = "^\\d{15}|\\d{18}$";

    final static String IDENTITY_NUMBER_18 = "^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{4}$";

    final static String IDENTITY_NUMBER_SHORT_1 = "^([0-9]){7,18}(x|X)?$";

    final static String IDENTITY_NUMBER_SHORT_2 = "^\\d{8,18}|[0-9x]{8,18}|[0-9X]{8,18}?$";

    final static String CHINESE = "^[\\u4e00-\\u9fa5]{0,}$";

    public static boolean isMobile(String text) {
        return text.matches(MOBILE);
    }

    public static boolean isPhone(String text) {
        return text.matches(PHONE);
    }

    public static boolean isEmail(String text) {
        return text.matches(EMAIL);
    }

    public static boolean isIdentityNumber(String text) {
        return text.matches(IDENTITY_NUMBER_15) || text.matches(IDENTITY_NUMBER_15_18) ||
                text.matches(IDENTITY_NUMBER_18) || text.matches(IDENTITY_NUMBER_SHORT_1) ||
                text.matches(IDENTITY_NUMBER_SHORT_2);
    }

    public static boolean isChinese(String text) {
        return text.matches(CHINESE);
    }
}

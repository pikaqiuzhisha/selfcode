package com.chargedot.charge.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/31
 */
public class StringUtil {

    /**
     * unicode转中文
     * @param str
     * @return string
     */
    public static String unicodeToString(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), ch+"" );
        }
        return str;
    }
}

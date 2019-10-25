package com.chargedot.wechat.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
public class RefundOrderNumberGenerator {
    private final static String STR_TIMESTAMP = "yyyyMMddHHmmss";

    private static final ThreadLocal<DateFormat> DF_TIMESTAMP = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat(STR_TIMESTAMP);
        }
    };

    /**
     * count
     */
    private int cnt;
    /**
     * minute
     */
    private long minute;

    private static RefundOrderNumberGenerator instance = new RefundOrderNumberGenerator();

    /**
     *
     */
    private RefundOrderNumberGenerator() {
    }

    public static RefundOrderNumberGenerator getInstance() {
        return instance;
    }

    /**
     * generate an id
     * @return 返回退款单号
     */
    public synchronized String generate(int deviceId) {
        long now = System.currentTimeMillis();
        long m = now / 1000 / 60 * 60 * 1000;
        if (minute < m) {
            minute = m;
            cnt = 0;
        }
        cnt++;
        Random random = new Random();
        int ran = random.nextInt(90000) + 10000;
        return "C" + DF_TIMESTAMP.get().format(new Date(now)) + ran + cnt;
    }
}

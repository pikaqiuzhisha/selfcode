package com.chargedot.refund.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
public class SequenceNumberGengerator {

    private final static String STR_TIMESTAMP = "yyyyMMddHHmmss";
    private static final ThreadLocal<DateFormat> DF_TIMESTAMP = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat(STR_TIMESTAMP);
        }
    };


    private static SequenceNumberGengerator instance = new SequenceNumberGengerator();

    /**
     *
     */
    private SequenceNumberGengerator() {
    }

    public static SequenceNumberGengerator getInstance() {
        return instance;
    }

    /**
     * generate an id
     *
     * @return
     */
    public synchronized String generate(long timeMillis, int userId, int couplerId) {
        return DF_TIMESTAMP.get().format(new Date(timeMillis)) + String.format("%08d", userId) + String.format("%08d", couplerId);
    }
}

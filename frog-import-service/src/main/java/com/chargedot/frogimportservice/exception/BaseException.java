package com.chargedot.frogimportservice.exception;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/3/19
 */
public class BaseException extends Exception {

    private static final long serialVersionUID = 8528585047994283813L;

    public BaseException() {
    }

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseException(Throwable cause) {
        super(cause);
    }

    public BaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

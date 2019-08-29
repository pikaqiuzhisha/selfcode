package com.chargedot.frogimportservice.exception;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/4/25
 */
public class InsertException extends BaseException {

    private static final long serialVersionUID = -1797787819503486020L;

    public InsertException() {
    }

    public InsertException(String message) {
        super(message);
    }

    public InsertException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsertException(Throwable cause) {
        super(cause);
    }

    public InsertException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

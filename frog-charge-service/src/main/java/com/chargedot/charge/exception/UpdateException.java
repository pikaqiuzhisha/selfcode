package com.chargedot.charge.exception;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/4/25
 */
public class UpdateException extends BaseException{
    private static final long serialVersionUID = -760715373468198949L;

    public UpdateException() {
    }

    public UpdateException(String message) {
        super(message);
    }

    public UpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpdateException(Throwable cause) {
        super(cause);
    }

    public UpdateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

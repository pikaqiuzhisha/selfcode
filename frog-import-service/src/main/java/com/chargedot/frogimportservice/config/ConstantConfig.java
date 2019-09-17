package com.chargedot.frogimportservice.config;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/3/16
 */
public class ConstantConfig {
    /**
     * return value
     */
    public static final Integer RET_SYSTEM_BUSY = -1;// 系统繁忙，稍后重试
    public static final Integer RET_SUCCESS = 0;// 成功
    public static final Integer RET_POST_PARAM_ERROR = 4003;// post参数错误
    public static final Integer RET_SYSTEM_ERROR = 500;// 系统错误


    /**
     * return message
     */
    public static final String MSG_SYSTEM_BUSY = "system busy, please try again later.";
    public static final String MSG_POST_PARAM_ERROR = "post param is illegal.";
    public static final String MSG_SYSTEM_ERROR = "system error, please try again later.";
    public static final String MSG_SUCCESS = "request success.";

}

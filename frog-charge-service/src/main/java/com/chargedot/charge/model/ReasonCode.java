package com.chargedot.charge.model;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/31
 */
public enum ReasonCode {

    SUCCESS(0, "正常"),
    OVER_CURRENT(1, "过流停充"),
    DEVICE_FAULT(2, "设备故障停充"),
    CHARGE_FAILURE(3, "充电失败"),
    USER_STOP(4, "用户主动停止"),
    FULL_STOP(5, "充满自动停止"),
    NO_PERMISSION(18, "无权限"),
    REFUSE_SERVICE(19, "拒绝服务"),
    SYSTEM_ERROR(80, "系统错误"),
    EXECUTE_FAILURE(81, "执行失败"),
    DEVICE_RESTART(60, "设备重启");
    ;

    private int statusCode;
    private String describe;

    public static String getType(int statusCode){
        for(ReasonCode reasonCode : ReasonCode.values()){
            if(reasonCode.getStatusCode() == statusCode){
                return reasonCode.describe;
            }
        }
        return "";
    }

    ReasonCode(int statusCode, String describe) {
        this.statusCode = statusCode;
        this.describe = describe;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}

package com.chargedot.charge.model;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/31
 */
public enum ReasonUserCode {

    SUCCESS(0, "正常"),
    OVER_CURRENT(1, "设备故障停充"),
    DEVICE_FAULT(2, "设备故障停充"),
    CHARGE_FAILURE(3, "充电失败"),
    USER_STOP(4, "正常"),
    FULL_STOP(5, "正常"),
    NO_PERMISSION(18, "充电失败"),
    REFUSE_SERVICE(19, "充电失败"),
    SYSTEM_ERROR(80, "充电失败"),
    EXECUTE_FAILURE(81, "充电失败"),
    DEVICE_RESTART(60, "设备重启");
    ;

    private int statusCode;
    private String describe;

    public static String getType(int statusCode){
        for(ReasonUserCode reasonCode : ReasonUserCode.values()){
            if(reasonCode.getStatusCode() == statusCode){
                return reasonCode.describe;
            }
        }
        return "";
    }

    ReasonUserCode(int statusCode, String describe) {
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

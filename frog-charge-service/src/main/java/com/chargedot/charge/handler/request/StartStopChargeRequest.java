package com.chargedot.charge.handler.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/17
 */
@Data
public class StartStopChargeRequest extends Request {

    /**
     * 设备端口号
     */
    @JsonProperty("Port")
    private String port;

    /**
     * 用户ID
     */
    @JsonProperty("CertId")
    private Integer certId;

    @JsonProperty("Type")
    private Integer type;

    /**
     * 凭证类型
     */
    @JsonProperty("CertType")
    private Integer certType;

    /**
     * 充电流水号对应时间戳（秒数值）
     */
    @JsonProperty("TimeStamp")
    private Integer timeStamp;

    /**
     * 启停状态，1-成功，2-失败
     */
    @JsonProperty("Status")
    private Integer status;

    /**
     * 具体原因:
     *   0-正常，1-过流停充，
     *   2-设备故障停充，3-充电失败，
     *   4-用户主动停止，5-充满自动停止，
     *   18-无权限，19-拒绝服务，
     *   80-系统错误，81-执行失败
     */
    @JsonProperty("Reason")
    private Integer reason;

    public void setter(String deviceNumber, String port, int type, int status, int reason, int userId, int timeStamp, Integer certTye) {
        this.setDeviceNumber(deviceNumber);
        this.setPort(port);
        this.setReason(reason);
        this.setType(type);
        this.setStatus(status);
        this.setTimeStamp(timeStamp);
        this.setCertId(userId);
        this.setCertType(certTye);
    }
}

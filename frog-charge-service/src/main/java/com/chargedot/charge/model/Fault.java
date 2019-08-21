package com.chargedot.charge.model;

import lombok.Data;

/**
 * @Author：caoj
 * @Description： malfunction model
 * @Data：Created in 2018/1/18
 */
@Data
public class Fault {


    /**
     * 当前时间戳+deviceID+六位随机数
     */
    private String id;

    /**
     * 枪端口号
     */
    private String portNumber;

    /**
     * 枪口号ID
     */
    private int portId;

    /**
     * 设备枪口号
     */
    private String port;

    /**
     * 设备ID
     */
    private Integer deviceId;

    /**
     * 设备类型，1:交流, 2:直流, 3:交直流一体机, 4:充电堆
     */
    private Integer deviceType;

    /**
     * 设备所属站点ID
     */
    private Integer stationId;

    /**
     * 错误类型
     */
    private Integer faultType;

    /**
     * 错误代码
     */
    private Integer faultCode;

    /**
     * 故障开始时间
     */
    private String startedAt;

    /**
     * 故障结束时间
     */
    private String finishedAt;

    /**
     * 是否删除
     */
    private Integer isDel;


}

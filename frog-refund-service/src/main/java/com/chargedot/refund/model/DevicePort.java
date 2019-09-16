package com.chargedot.refund.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Data
public class DevicePort {

    private Integer id;

    private String portNumber;

    private String portName;

    private Integer stationId;

    private Integer deviceId;

    private Integer tryOccupyUserId;

    private Integer status;

    private String detail;

    private String feeDetail;

}

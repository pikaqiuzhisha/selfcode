package com.chargedot.charge.handler.request;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Data
public class StartChargeRequest extends StartStopChargeRequest {

    /**
     * 预设充电时长.单位分钟
     */
    private Integer presetChargeTime;
}

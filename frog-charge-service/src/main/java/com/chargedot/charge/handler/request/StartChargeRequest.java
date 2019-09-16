package com.chargedot.charge.handler.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("PresetChargeTime")
    private Integer presetChargeTime;
}

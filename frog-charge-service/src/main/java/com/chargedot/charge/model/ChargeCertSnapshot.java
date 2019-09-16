package com.chargedot.charge.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Data
public class ChargeCertSnapshot {

    /**
     * ID
     */
    private Long id;

    /**
     * 凭证号
     */
    private String certNumber;

    /**
     * 当前剩余有效次数或者有效时长，或者虚拟账户余额
     */
    private Integer curValue;

    /**
     * 实账余额
     */
    private Integer realValue;

    /**
     * 使用状态
     */
    private Integer certStatus;



}

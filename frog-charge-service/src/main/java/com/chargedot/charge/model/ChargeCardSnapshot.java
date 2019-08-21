package com.chargedot.charge.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Data
public class ChargeCardSnapshot {

    /**
     * ID
     */
    private Long id;

    /**
     * 卡号
     */
    private String cardNumber;

    /**
     * 当前剩余有效次数或者有效时长
     */
    private Integer curValue;

    /**
     * 卡片使用状态
     */
    private Integer cardStatus;



}

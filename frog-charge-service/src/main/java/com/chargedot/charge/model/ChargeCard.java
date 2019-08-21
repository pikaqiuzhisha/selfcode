package com.chargedot.charge.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Data
public class ChargeCard {

    /**
     * ID
     */
    private Long id;

    /**
     * 卡号
     */
    private String cardNumber;

    /**
     * 卡用户ID
     */
    private Integer userId;

    /**
     * 卡类型（卡类型默认1包月次卡 2包月包时卡 3充值卡）
     */
    private Integer type;

    /**
     * 卡有效期开始时间
     */
    private String beginedAt;

    /**
     * 卡有效期结束时间
     */
    private String finishedAt;

    /**
     * 最大使用次数或者最大可用时长
     */
    private Integer maxChargeValue;

    /**
     * 当前剩余有效次数或者有效时长
     */
    private Integer curValue;

    /**
     * 卡片封禁状态（默认1已激活2封禁）
     */
    private Integer forbidStatus;

    /**
     * 卡片使用状态（默认1可使用2使用中3不可使用）
     */
    private Integer cardStatus;



}

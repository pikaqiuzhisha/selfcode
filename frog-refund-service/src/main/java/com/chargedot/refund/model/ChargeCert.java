package com.chargedot.refund.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Data
public class ChargeCert {

    /**
     * ID
     */
    private Long id;

    /**
     * 充电凭证
     */
    private String certNumber;

    /**
     * 所属用户ID
     */
    private int enterpriseId;

    /**
     * 关联用户ID
     */
    private Integer userId;

    /**
     * 凭证类型（1包月次卡 2包月包时卡 3充值卡 4手机号）
     */
    private Integer type;

    /**
     * 有效期开始时间
     */
    private String beginedAt;

    /**
     * 有效期结束时间
     */
    private String finishedAt;

    /**
     * 最大使用次数或者最大可用时长
     */
    private Integer maxValue;

    /**
     * 当前剩余有效次数或者有效时长，或者虚拟账户余额
     */
    private Integer curValue;

    /**
     * 实账余额
     */
    private Integer realValue;

    /**
     * 卡片封禁状态（默认1已激活2封禁）
     */
    private Integer forbidStatus;

    /**
     * 使用状态（1可使用 2使用中 3不可使用）
     */
    private Integer certStatus;

    private Integer cardStatus;

}

package com.chargedot.wechat.model;

import lombok.Data;

@Data
public class RefundRecord {

    /**
     * 退款单号id
     */
    private Integer id;

    /**
     * 退款单号
     */
    private String refundNumber;

    /**
     * 订单id
     */
    private Integer orderId;

    /**
     * 订单号
     */
    private String orderNumber;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 退款金额
     */
    private Integer refundMoney;

    /**
     * 退款操作人 默认0系统 其他系统用户id
     */
    private Integer refundOprId;

    /**
     * 退款状态 1待退款 2退款成功 3退款失败 4待审核 5审核中 6审核通过 7审核不通过
     */
    private Integer refundStatus;

    /**
     * 退款类型 默认0系统 1平台
     */
    private Integer refundType;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 退款时间
     */
    private String refundAt;

    public void refundSetter(String refundNumber, Integer orderId, Integer userId,
                             Integer refundMoney, Integer refundOprId, Integer refundStatus,
                             Integer refundType, String refundReason) {
        this.setRefundNumber(refundNumber);
        this.setOrderId(orderId);
        this.setUserId(userId);
        this.setRefundMoney(refundMoney);
        this.setRefundOprId(refundOprId);
        this.setRefundStatus(refundStatus);
        this.setRefundType(refundType);
        this.setRefundReason(refundReason);
    }
}

package com.chargedot.wechat.controller.vo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RefundRecord {
    @NotNull
    private Long id;

    @NotNull
    @XStreamAlias("refund_order")
    private String refundOrder;

    @NotNull
    @XStreamAlias("order_number")
    private String orderNumber;

    @NotNull
    @XStreamAlias("user_id")
    private Integer userID;

    @NotNull
    @XStreamAlias("stream_id")
    private Long streamId;

    @NotNull
    @XStreamAlias("refund_money")
    private Integer refundMoney;

    @NotNull
    @XStreamAlias("refund_opr_id")
    private Integer refundOprId;

    @NotNull
    @XStreamAlias("refund_status")
    private Integer refundStatus;

    @NotNull
    @XStreamAlias("refund_type")
    private Integer refundType;

    @XStreamAlias("refund_reason")
    private String refundReason;

    @NotNull
    @XStreamAlias("refund_at")
    private String refundAt;

    public RefundRecord() {
    }

    public RefundRecord(Long id, String refundOrder, String orderNumber, Integer userID, Long streamId, Integer refundMoney, Integer refundOprId, Integer refundStatus, Integer refundType, String refundReason, String refundAt) {
        this.id = id;
        this.refundOrder = refundOrder;
        this.orderNumber = orderNumber;
        this.userID = userID;
        this.streamId = streamId;
        this.refundMoney = refundMoney;
        this.refundOprId = refundOprId;
        this.refundStatus = refundStatus;
        this.refundType = refundType;
        this.refundReason = refundReason;
        this.refundAt = refundAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefundOrder() {
        return refundOrder;
    }

    public void setRefundOrder(String refundOrder) {
        this.refundOrder = refundOrder;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(Long streamId) {
        this.streamId = streamId;
    }

    public Integer getRefundMoney() {
        return refundMoney;
    }

    public void setRefundMoney(Integer refundMoney) {
        this.refundMoney = refundMoney;
    }

    public Integer getRefundOprId() {
        return refundOprId;
    }

    public void setRefundOprId(Integer refundOprId) {
        this.refundOprId = refundOprId;
    }

    public Integer getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(Integer refundStatus) {
        this.refundStatus = refundStatus;
    }

    public Integer getRefundType() {
        return refundType;
    }

    public void setRefundType(Integer refundType) {
        this.refundType = refundType;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getRefundAt() {
        return refundAt;
    }

    public void setRefundAt(String refundAt) {
        this.refundAt = refundAt;
    }
}

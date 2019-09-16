package com.chargedot.charge.model;

import com.chargedot.charge.config.ConstantConfig;
import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Data
public class ChargeOrder {

    private Integer id;

    private String orderNumber;

    private Integer userId;

    private Long certId;

    private  Integer portId;

    private Integer deviceId;

    private Integer stationId;

    private String sequenceNumber;

    /**
     * 启动方式：默认1为微信2为app3为卡
     */
    private Integer startType;

    private String startDetail;

    private String startedAt;

    private String finishedAt;

    /**
     * 实际充电时长
     */
    private Integer duration;

    /**
     * 预设充电时长
     */
    private Integer durationPlan;

    /**
     * 扣款来源（1实账2虚账3虚实结合）
     */
    private Integer paySrc;

    /**
     * 预收费用
     */
    private Integer payment;

    /**
     * 实帐支付费用
     */
    private Integer paymentAct;

    /**
     * 虚拟账户支付费用
     */
    private Integer virtualPayment;

    /**
     * 退款总金额
     */
    private Integer refundAct;

    /**
     * 虚账退款金额
     */
    private Integer virtualRefund;

    /**
     * 订单类型：默认1正常2异常3校正
     */
    private Integer orderType;

    /**
     * 支付状态：默认1为待支付,2为支付成功,3为支付失败,4已退款,5已开票,6待退款,7人工代支付(包含人工扣次数，人工扣时长，人工扣余额)
     */
    private Integer payStatus;

    /**
     * 订单状态：默认1为已创建,2为进行中,3为正常充电完成,4为过流停止充电
     */
    private Integer orderStatus;

    /**
     * 停充原因
     */
    private String chargeFinishReason;

    /**
     * 支付方式：默认1为余额支付,2支付宝支付,3微信支付,4月卡支付,5平台扣款，6站点卡支付，7优惠卡（满几次送几次的卡）8包月包时卡9包月包次卡10充值卡
     */
    private Integer payType;

    /**
     * 付款流水ID
     */
    private Long payStreamId;

    /**
     * 退款流水ID
     */
    private Long refundStreamId;

    /**
     * 订单创建时用户费率
     */
    private String feeDetailSnap;

    /**
     * 付款时间
     */
    private String payedOrderAt;

    /**
     * 退款时间
     */
    private String refundOrderAt;

    public void startSetter(int portId, int deviceId, int stationId, long certId, int userId, String startDetail, String sequenceNumber, String orderNumber, int presetChargeTime) {
        this.setOrderNumber(orderNumber);
        this.setSequenceNumber(sequenceNumber);
        this.setCertId(certId);
        this.setUserId(userId);
        this.setPortId(portId);
        this.setDeviceId(deviceId);
        this.setStationId(stationId);
        this.setDurationPlan(presetChargeTime);
        this.setStartDetail(startDetail);
        this.setOrderStatus(ConstantConfig.ONGOING);
        this.setStartType(ConstantConfig.START_BY_CARD);
    }

    public void paySetter(int payment, int paymentAct, int virtualPayment, int paySrc, int payStatus, int payType, String feeDetailSnap, String payedOrderAt) {
        this.setPayment(payment);
        this.setPaymentAct(paymentAct);
        this.setVirtualPayment(virtualPayment);
        this.setPaySrc(paySrc);
        this.setPayStatus(payStatus);
        this.setPayType(payType);
        this.setFeeDetailSnap(feeDetailSnap);
        this.setOrderStatus(ConstantConfig.ONGOING);
        this.setPayedOrderAt(payedOrderAt);
    }

    /**
     *
     * @return
     */
    public boolean isOnGoing() {
        return ConstantConfig.ONGOING == this.getOrderStatus();
    }
}

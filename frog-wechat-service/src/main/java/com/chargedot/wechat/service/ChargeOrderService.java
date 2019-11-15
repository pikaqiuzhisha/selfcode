package com.chargedot.wechat.service;

import com.chargedot.wechat.model.ChargeOrder;

public interface ChargeOrderService {

    /**
     * 根据订单编号找到最近一条订单信息
     *
     * @param orderId 订单编号号
     * @return 返回最近一条的订单信息
     */
    ChargeOrder findByOrderID(Integer orderId);

    /**
     * 根据订单号找到最近一条订单信息
     *
     * @param orderNumber 订单号
     * @return 返回最近一条的订单信息
     */
    ChargeOrder findByOrderNumber(String orderNumber);

    /**
     * 更新订单状态
     *
     * @param chargeOrder 订单对象
     */
    void updateChargeOrder(ChargeOrder chargeOrder);
}

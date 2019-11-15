package com.chargedot.wechat.mapper;

import com.chargedot.wechat.model.ChargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChargeOrderMapper {

    /**
     * 根据订单编号找到最近一条订单信息
     *
     * @param orderId 订单编号
     * @return 返回最近一条的订单信息
     */
    ChargeOrder findByOrderID(@Param("orderId") Integer orderId);

    /**
     * 根据订单号找到最近一条订单信息
     *
     * @param orderNumber 订单号
     * @return 返回最近一条的订单信息
     */
    ChargeOrder findByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 更新订单状态
     *
     * @param chargeOrder 订单对象
     */
    void updateChargeOrder(ChargeOrder chargeOrder);
}

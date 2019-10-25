package com.chargedot.wechat.controller;

import com.chargedot.wechat.config.ConstantConfig;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.model.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.chargedot.wechat.util.RefundOrderNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/refund")
public class RefundRecordController {

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private ChargeOrderService chargeOrderService;

    @RequestMapping(value = "/refund_money", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> refund(@RequestParam String orderNumber) {
        log.debug("[orderNumber]：{}", orderNumber);
        //声明一个订单对象
        ChargeOrder order = new ChargeOrder();
        //声明一个退款单对象
        RefundRecord refundRecord = new RefundRecord();

        //异常处理
        try {
            //校验参数是否有值
            if (StringUtils.isBlank(orderNumber)) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "订单号为空.", null), HttpStatus.OK);
            }

            order.setOrderNumber(orderNumber);
            order.setOrderStatus(ConstantConfig.UNCHARGEING);
            order.setPayStatus(ConstantConfig.UNREFUND);

            //更新订单的订单状态和充电状态
            chargeOrderService.updateChargeOrder(order);

            //查找最近一条订单信息
            ChargeOrder chargeOrder = chargeOrderService.findByOrderNumber(orderNumber);

            //校验是否存在该订单信息
            if (Objects.isNull(chargeOrder)) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "没有改订单号的订单信息.", null), HttpStatus.OK);
            }

            String refundNumber = RefundOrderNumberGenerator.getInstance().generate(chargeOrder.getId());

            refundRecord.refundSetter(refundNumber, chargeOrder.getId(), chargeOrder.getUserId(), chargeOrder.getRefundAct(),
                    0, ConstantConfig.UNREFUND, 0, null);

            refundRecordService.insertRefundRecord(refundRecord);

            // TODO 调微信接口

            refundRecord.setRefundStatus(ConstantConfig.REFUND);
            refundRecordService.updateRefundRecord(refundRecord);

            order.setOrderStatus(ConstantConfig.FINISH_SUCCESS);
            order.setPayStatus(ConstantConfig.REFUND);
            //更新订单的订单状态和充电状态
            chargeOrderService.updateChargeOrder(order);
        } catch (Exception ex) {
            log.warn("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息：" + ex.getMessage(), null), HttpStatus.OK);
        }
        return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, ".", null), HttpStatus.OK);
    }


}

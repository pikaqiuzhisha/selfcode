package com.chargedot.wechat.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.chargedot.wechat.config.ConstantConfig;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.model.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/refund_notify")
public class RefundNotifyController {

    @Autowired
    private WxPayService payService;

    @Autowired
    private ChargeOrderService chargeOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Transactional
    @RequestMapping(value = "/wx_notify", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String parseRefundNotifyResult(@RequestBody String xmlData) throws WxPayException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            log.info("微信支付退款异步通知参数：{}", xmlData);
            WxPayRefundNotifyResult result = WxPayRefundNotifyResult.fromXML(xmlData, payService.getConfig().getMchKey());
            log.info("微信支付退款异步通知解析后的对象：{}", JSONUtils.toJSONString(result));

            // 查找最近一条订单信息
            ChargeOrder chargeOrder = chargeOrderService.findByOrderNumber(result.getReqInfo().getOutTradeNo());
            // 校验
            if (Objects.isNull(chargeOrder)) {
                return WxPayNotifyResponse.fail("该订单不存在");
            }
            // 是否是重复退款通知
            if (ConstantConfig.REFUND == chargeOrder.getPayStatus()) {
                return WxPayNotifyResponse.success("该订单已退款,无需再退款");
            }

            // 更新退款记录表的退款状态
            RefundRecord refundRecord = refundRecordService.getRefundRecordByRefundNumber(result.getReqInfo().getOutRefundNo());
            // 校验
            if (Objects.isNull(refundRecord)) {
                return WxPayNotifyResponse.fail("该退款单不存在");
            }
            refundRecord.setRefundStatus(ConstantConfig.REFUND);
            refundRecord.setRefundAt(sdf.format(result.getReqInfo().getSuccessTime()));
            refundRecordService.updateRefundRecord(refundRecord);
            // 更改订单表订单状态
            chargeOrder.setPayStatus(ConstantConfig.REFUND);
            chargeOrderService.updateChargeOrder(chargeOrder);
            return WxPayNotifyResponse.success("成功");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WxPayException("发生异常，" + e.getMessage(), e);
        }
    }
}

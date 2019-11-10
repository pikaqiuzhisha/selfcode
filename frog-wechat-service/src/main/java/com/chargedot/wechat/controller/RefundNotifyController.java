package com.chargedot.wechat.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.chargedot.wechat.config.ConstantConfig;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.model.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.chargedot.wechat.util.RefundOrderNumberGenerator;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/refundnotify")
public class RefundNotifyController {

    @Autowired
    private WxPayService payService;

    @Autowired
    private ChargeOrderService chargeOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Transactional
    @RequestMapping(value = "/refund_notify", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String parseRefundNotifyResult(String xmlData) throws WxPayException {
        try {
            log.info("微信支付退款异步通知参数：{}", xmlData);
            WxPayRefundNotifyResult result = WxPayRefundNotifyResult.fromXML(xmlData, payService.getConfig().getMchKey());
            log.info("微信支付退款异步通知解析后的对象：{}", JSONUtils.toJSONString(result));
            //判断是否是重复通知
            //查找最近一条订单信息
            ChargeOrder chargeOrder = chargeOrderService.findByOrderNumber(result.getReqInfo().getOutTradeNo());

            //是否是重复退款通知
            if (ConstantConfig.REFUND ==chargeOrder.getPayStatus()) {
                return WxPayNotifyResponse.success("成功");
            }
            //更新数据库退款状态
            //更新订单表数据库退款状态
            chargeOrder.setPayStatus(ConstantConfig.REFUND);
            chargeOrderService.updateChargeOrder(chargeOrder);

            //更新插入退款记录表
            RefundRecord refundRecord = refundRecordService.getRefundRecordByOrderId(result.getReqInfo().getOutTradeNo());
            refundRecord.setRefundStatus(ConstantConfig.REFUND);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            refundRecord.setRefundAt(sdf.format(result.getReqInfo().getSuccessTime()));
            refundRecord.setRefundMoney(result.getReqInfo().getSettlementRefundFee());
            refundRecordService.updateRefundRecord(refundRecord);

            return WxPayNotifyResponse.success("成功");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WxPayException("发生异常，" + e.getMessage(), e);
        }
    }
}

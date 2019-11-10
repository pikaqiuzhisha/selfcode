package com.chargedot.wechat.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.chargedot.wechat.config.ConstantConfig;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.model.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.chargedot.wechat.util.RefundOrderNumberGenerator;
import com.github.binarywang.wxpay.bean.request.WxPayRefundQueryRequest;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/refundquery")
public class RefundQueryController {
    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private ChargeOrderService chargeOrderService;

    @Autowired
    private WxPayService payService;

    @Autowired
    private WxPayService wxService;

    @RequestMapping(value = "/refund_money", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> refundQuery(@RequestBody WxPayRefundQueryRequest wxPayRefundQueryRequest) {
        try{
            //校验参数
            if (StringUtils.isEmpty(wxPayRefundQueryRequest.getOutTradeNo())) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(ConstantConfig.RET_POST_PARAM_ERROR, "参数错误", null), HttpStatus.BAD_REQUEST);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            log.info("[wxPayRefundRequest]：{}", JSONUtils.toJSONString(wxPayRefundQueryRequest));
            WxPayRefundQueryResult wxPayRefundQueryResult = wxService.refundQuery(wxPayRefundQueryRequest);
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "" , wxPayRefundQueryResult), HttpStatus.OK);

        } catch (Exception ex) {
            log.warn("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息" + ex.getMessage(), null), HttpStatus.OK);
        }

    }
}

package com.chargedot.wechat.controller;

import com.chargedot.wechat.controller.vo.CommonResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@RequestMapping("/refund_query")
public class RefundQueryController {
    @Autowired
    private WxPayService wxPayService;

    @RequestMapping(value = "/wx_query", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> refundQuery(@RequestParam String OrderNumber) {

        try {
            WxPayRefundQueryResult wxPayRefundQueryResult = wxPayService.refundQuery(null, OrderNumber, null, null);
            log.info(wxPayRefundQueryResult.toString());

        } catch (Exception ex) {
            log.warn("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息" + ex.getMessage(), null), HttpStatus.OK);
        }
        return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "等待退款", "wxPayRefundQueryResult"), HttpStatus.OK);

    }
}



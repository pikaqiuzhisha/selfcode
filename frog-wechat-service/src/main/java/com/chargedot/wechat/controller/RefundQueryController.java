
package com.chargedot.wechat.controller;
import com.alibaba.druid.support.json.JSONUtils;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.chargedot.wechat.controller.vo.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.chargedot.wechat.util.JacksonUtil;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;

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
    private WxPayService wxPayService;
    @RequestMapping(value = "/wx_query", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> refundQuery(@RequestBody String refundRecord) {

        //JSONObject转成JAVA对象
        RefundRecord rr = JacksonUtil.json2Bean(refundRecord, RefundRecord.class);
       /* SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            log.info("[wxPayRefundRequest]：{}", JSONUtils.toJSONString(rqd));*/
        try {
            WxPayRefundQueryResult wxPayRefundQueryResult = wxPayService.refundQuery(null, rr.getOrderNumber(), null, null);
            log.info(wxPayRefundQueryResult.toString());

        } catch (Exception ex) {
            log.warn("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息" + ex.getMessage(), null), HttpStatus.OK);
        }
        return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "等待退款" , null), HttpStatus.OK);

    }
}



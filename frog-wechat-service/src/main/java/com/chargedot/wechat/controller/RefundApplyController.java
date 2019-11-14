package com.chargedot.wechat.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.chargedot.wechat.controller.vo.RefundRecord;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.util.JacksonUtil;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.text.SimpleDateFormat;

@RestController
@Slf4j
@RequestMapping("/refundapply")
public class RefundApplyController {
    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private ChargeOrderService chargeOrderService;

    @Autowired
    private WxPayService payService;




    @RequestMapping(value = "/wx_apply", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> refund(@RequestBody String refundRecord) {
//        payService.getConfig().setUseSandboxEnv(true);

        //校验参数
        if (org.apache.commons.lang3.StringUtils.isBlank(refundRecord)) {
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "参数错误.", null), HttpStatus.OK);
        }
        //JSONObject转成JAVA对象
        RefundRecord rr = JacksonUtil.json2Bean(refundRecord, RefundRecord.class);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        //异常处理
        try {
           /*
         //微信方式校验是否重复退款
           WxPayRefundQueryRequest wxPayRefundQueryRequest = new WxPayRefundQueryRequest();
            BeanUtils.copyProperties(rd,WxPayRefundQueryData);
            WxPayRefundQueryResult wxPayRefundQueryResult = wxService.refundQuery(wxPayRefundQueryRequest);
            List<WxPayRefundQueryResult.RefundRecord> refundRecords = wxPayRefundQueryResult.getRefundRecords();
            for (WxPayRefundQueryResult.RefundRecord record : refundRecords) {
//                退款状态：SUCCESS—退款成功 REFUNDCLOSE—退款关闭。
                if ("SUCCESS".equals(record.getRefundStatus())||"REFUNDCLOSE".equals(record.getRefundStatus())) {
                    return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "请不要重复退款.", null), HttpStatus.OK);
                }
            }*/

/*
            //校验退款金额是否合理
            if (Integer.compare(rd.getTotalFee(),rd.getRefundFee()) == -1) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "退款总金额大于订单金额.", null), HttpStatus.OK);
            }*/
            ChargeOrder order = chargeOrderService.findByOrderNumber(rr.getOrderNumber());
            //开始退款
            /**
             * 调微信申请退款接口
             */
            WxPayRefundRequest request = WxPayRefundRequest.newBuilder()
                    .outRefundNo(rr.getRefundOrder())
                    .outTradeNo(rr.getOrderNumber())
                    .totalFee(100)
                    .refundFee(rr.getRefundMoney())
                    .notifyUrl("http://forgpay.natapp1.cc/refundnotify/wx_notify")
                    .build();
            WxPayRefundResult result = this.payService.refund(
                    request);
          //  log.info("[WxPayRefundResult]：{}", JSONUtils.toJSONString(result));
            System.out.println(result.getReturnCode());

            if (!"SUCCESS".equals(result.getReturnCode())) {
                String returnMsg = result.getReturnMsg();
                if (Strings.isEmpty(returnMsg))
                    returnMsg = "微信退款申请业务提交失败";
                return new ResponseEntity<CommonResult>(
                        CommonResult.buildResults(1, returnMsg, null), HttpStatus.OK);
            }
            if (!"SUCCESS".equals(result.getResultCode())) {
                String errorMsg = result.getErrCodeDes()+"[error_code-"+result.getErrCode()
                        +"]";
                if (Strings.isEmpty(errorMsg))
                    errorMsg = "微信退款申请业务提交失败";
                return new ResponseEntity<CommonResult>(
                        CommonResult.buildResults(1, errorMsg, null), HttpStatus.OK);
            }

           /* //更新订单表数据库退款状态
            chargeOrder.setOrderStatus(ConstantConfig.FINISH_SUCCESS);
            chargeOrder.setPayStatus(ConstantConfig.REFUNDING);
            //更新订单的订单状态和充电状态
            chargeOrderService.updateChargeOrder(chargeOrder);

            //插入退款记录表
            String refundNumber = RefundOrderNumberGenerator.getInstance().generateRefundOrder();
            refundRecord.refundSetter(refundNumber, chargeOrder.getId(), chargeOrder.getUserId(), chargeOrder.getRefundAct(),
                    0, ConstantConfig.UNREFUND, 0, null);
            refundRecord.setRefundStatus(ConstantConfig.REFUNDING);
            refundRecord.setRefundAt(sdf.format(new Date()));
            refundRecordService.insertRefundRecord(refundRecord);
*/
        } catch (Exception ex) {
            log.info("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息" + ex.getMessage(), null), HttpStatus.OK);
        }
        return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "进入退款中状态",null ), HttpStatus.OK);
    }
}

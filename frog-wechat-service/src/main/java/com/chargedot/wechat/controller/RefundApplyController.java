package com.chargedot.wechat.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.chargedot.wechat.config.ConstantConfig;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.github.binarywang.wxpay.bean.request.WxPayRefundQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.model.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.chargedot.wechat.util.RefundOrderNumberGenerator;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    @Autowired
    private WxPayService wxService;

    @RequestMapping(value = "/refund_money", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> refund(@RequestBody WxPayRefundRequest wxPayRefundRequest) {
        //校验参数
        if (StringUtils.isEmpty(wxPayRefundRequest.getOutTradeNo()) ||
            StringUtils.isEmpty(wxPayRefundRequest.getTotalFee().toString()) ||
            StringUtils.isEmpty(wxPayRefundRequest.getRefundFee().toString())) {
            new ResponseEntity<CommonResult>(CommonResult.buildResults(ConstantConfig.RET_POST_PARAM_ERROR, "参数错误",null ), HttpStatus.BAD_REQUEST);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        log.info("[wxPayRefundRequest]：{}", JSONUtils.toJSONString(wxPayRefundRequest));
        //声明一个退款单对象
        RefundRecord refundRecord = new RefundRecord();
        //异常处理
        try {
            //查找最近一条订单信息
            ChargeOrder chargeOrder = chargeOrderService.findByOrderNumber(wxPayRefundRequest.getOutTradeNo());
            //校验是否存在该订单信息
            if (Objects.isNull(chargeOrder)) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "没有该订单号的订单信息.", null), HttpStatus.OK);
            }
            //是否是重复退款
            if (ConstantConfig.REFUND ==chargeOrder.getPayStatus()) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "请不要重复退款.", null), HttpStatus.OK);
            }
            WxPayRefundQueryRequest wxPayRefundQueryRequest = new WxPayRefundQueryRequest();
            BeanUtils.copyProperties(wxPayRefundRequest,wxPayRefundQueryRequest);
            WxPayRefundQueryResult wxPayRefundQueryResult = wxService.refundQuery(wxPayRefundQueryRequest);
            List<WxPayRefundQueryResult.RefundRecord> refundRecords = wxPayRefundQueryResult.getRefundRecords();
            for (WxPayRefundQueryResult.RefundRecord record : refundRecords) {
//                退款状态：SUCCESS—退款成功 REFUNDCLOSE—退款关闭。
                if ("SUCCESS".equals(record.getRefundStatus())||"REFUNDCLOSE".equals(record.getRefundStatus())) {
                    return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "请不要重复退款.", null), HttpStatus.OK);
                }
            }


            //校验退款金额是否合理
            if (Integer.compare(wxPayRefundRequest.getTotalFee(),wxPayRefundRequest.getRefundFee()) == -1) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "退款总金额大于订单金额.", null), HttpStatus.OK);
            }

            //开始退款
            /**
             * 调微信申请退款接口
             */
          /*  RefundRequest request = new RefundRequest();
            request.setOrderId(chargeOrder.getOrderNumber());
            request.setPayTypeEnum(BestPayTypeEnum.WXPAY_MINI);
            request.setOrderAmount((double) chargeOrder.getRefundAct());
            response = bestPayService.refund(request);*/
            WxPayRefundResult result = this.payService.refund(
                    WxPayRefundRequest.newBuilder()
                            .outRefundNo(chargeOrder.getRefundStreamId().toString())
                            .outTradeNo(wxPayRefundRequest.getOutTradeNo())
                            .totalFee(wxPayRefundRequest.getTotalFee())
                            .refundFee(wxPayRefundRequest.getRefundFee())
                            .build());
            log.info("[WxPayRefundResult]：{}", JSONUtils.toJSONString(result));
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

            //更新订单表数据库退款状态
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

        } catch (Exception ex) {
            log.info("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息" + ex.getMessage(), null), HttpStatus.OK);
        }
        return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "进入退款中状态",null ), HttpStatus.OK);
    }
}

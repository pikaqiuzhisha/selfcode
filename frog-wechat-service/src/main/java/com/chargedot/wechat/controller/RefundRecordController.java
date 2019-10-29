package com.chargedot.wechat.controller;

import com.chargedot.wechat.config.ConstantConfig;
import com.chargedot.wechat.controller.vo.CommonResult;
import com.chargedot.wechat.model.ChargeOrder;
import com.chargedot.wechat.model.RefundRecord;
import com.chargedot.wechat.service.ChargeOrderService;
import com.chargedot.wechat.service.RefundRecordService;
import com.chargedot.wechat.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        log.debug("[orderNumber]：{}", orderNumber);
        //声明一个退款单对象
        RefundRecord refundRecord = new RefundRecord();

        //异常处理
        try {
            //校验参数是否有值
            if (StringUtils.isBlank(orderNumber)) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "订单号为空.", null), HttpStatus.OK);
            }

            //查找最近一条订单信息
            ChargeOrder chargeOrder = chargeOrderService.findByOrderNumber(orderNumber);

            //校验是否存在该订单信息
            if (Objects.isNull(chargeOrder)) {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "没有该订单号的订单信息.", null), HttpStatus.OK);
            }

            chargeOrder.setOrderStatus(ConstantConfig.UNCHARGEING);
            chargeOrder.setPayStatus(ConstantConfig.UNREFUND);

            //更新订单的订单状态和充电状态
            chargeOrderService.updateChargeOrder(chargeOrder);

            String refundNumber = RefundOrderNumberGenerator.getInstance().generateRefundOrder();

            refundRecord.refundSetter(refundNumber, chargeOrder.getId(), chargeOrder.getUserId(), chargeOrder.getRefundAct(),
                    0, ConstantConfig.UNREFUND, 0, null);

            refundRecordService.insertRefundRecord(refundRecord);

            // TODO 微信接口
//            try{
//                KeyStore keyStore = KeyStore.getInstance("PKCS12");
//                FileInputStream inputStream = new FileInputStream(new File("E:\\wx\\apiclient_cert.pem"));
//                try{
//                    keyStore.load(inputStream, WXPayConstants.MCH_ID.toCharArray());
//                }catch (Exception e){
//                    e.printStackTrace();
//                }finally {
//                    inputStream.close();
//                }
//                SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, WXPayConstants.MCH_ID.toCharArray()).build();
//                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
//                        sslcontext, new String[] { "TLSv1" }, null,
//                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//                CloseableHttpClient httpclient = HttpClients.custom()
//                        .setSSLSocketFactory(sslsf).build();
//                HttpPost httppost = new HttpPost("https://api.mch.weixin.qq.com/secapi/pay/refund");
//                String xml = WXPayUtil.wxPayRefund(chargeOrder.getOrderNumber(), "410110111123435671", refundNumber, chargeOrder.getPayment().toString(), String.valueOf(chargeOrder.getRefundAct() *100));
//                try{
//                    StringEntity se = new StringEntity(xml);
//                    httppost.setEntity(se);
//                    System.out.println("executing request" + httppost.getRequestLine());
//                    CloseableHttpResponse responseEntry = httpclient.execute(httppost);
//                    try{
//                        HttpEntity entity = responseEntry.getEntity();
//                        System.out.println(responseEntry.getStatusLine());
//                        if (entity != null) {
//
//                        }
//                    }catch (Exception e){
//
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }

            // TODO 调微信接口
            String param = WXPayUtil.wxPayRefund(chargeOrder.getOrderNumber(), "410110111123435671", refundNumber, chargeOrder.getPayment().toString(), chargeOrder.getRefundAct().toString());
            log.debug("param{}", param);
            String result = "";
            String url = "https://api.mch.weixin.qq.com/secapi/pay/refund";
            try {
                result = WXPayUtil.wxPayBack(url, param);
            } catch (Exception ex) {
                log.warn("Exception{}", ex.getMessage());
                ex.printStackTrace();
            }

            Pattern pattern = Pattern.compile("\\.*(\\w{" + refundNumber.length() + "})\\.*");
            int st = result.indexOf("<refund_id>");
            String res = "";
            if (st >= 0) {
                int en = result.indexOf("</refund_id>");
                res = result.substring(st, en);
                Matcher m = pattern.matcher(res);
                if (m.find()) {
                    res = m.group(1);
                }
                if (res.length() > 0) {
                    result = "code:1,msg:退款成功";
                } else {
                    result = "code:-1,msg:退款失败";
                }
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "没有该订单号的订单信息.", result), HttpStatus.OK);
            }

            refundRecord.setRefundStatus(ConstantConfig.REFUND);
            refundRecord.setRefundAt(sdf.format(new Date()));
            refundRecordService.updateRefundRecord(refundRecord);

            chargeOrder.setOrderStatus(ConstantConfig.FINISH_SUCCESS);
            chargeOrder.setPayStatus(ConstantConfig.REFUND);
            //更新订单的订单状态和充电状态
            chargeOrderService.updateChargeOrder(chargeOrder);
        } catch (Exception ex) {
            log.warn("[Exception]：{}", ex.getMessage());
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "异常信息：" + ex.getMessage(), null), HttpStatus.OK);
        }
        return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "退款成功.", null), HttpStatus.OK);
    }


}

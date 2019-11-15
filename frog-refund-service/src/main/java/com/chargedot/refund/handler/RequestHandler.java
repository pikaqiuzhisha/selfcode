/**
 *
 */
package com.chargedot.refund.handler;

import com.chargedot.refund.config.ConstantConfig;
import com.chargedot.refund.config.ServerConfig;
import com.chargedot.refund.handler.request.CheckInRequest;
import com.chargedot.refund.handler.request.Request;
import com.chargedot.refund.handler.request.StartChargeRequest;
import com.chargedot.refund.handler.request.StopChargeRequest;
import com.chargedot.refund.mapper.*;
import com.chargedot.refund.model.*;
import com.chargedot.refund.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author gmm
 */
@Slf4j
public class RequestHandler {

    /**
     * event queue
     */
    private BlockingQueue<Request> queue;
    /**
     * event consumer thread pool
     */
    private ExecutorService threadPool;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ChargeCertMapper chargeCertMapper;

    @Autowired
    private DevicePortMapper devicePortMapper;

    @Autowired
    private ChargeOrderMapper chargeOrderMapper;

    @Autowired
    private ChargeStreamMapper chargeStreamMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    RestTemplate restTemplate;

    /**
     *
     */
    public RequestHandler() {
    }

    /**
     * initialize
     */
    @PostConstruct
    public void init() {
        queue = new LinkedBlockingQueue<Request>(serverConfig.requestMessageQueueCapacity);

        log.info("start request handle thread pool");
        threadPool = Executors.newFixedThreadPool(serverConfig.requestMessageHandlerCount);
        for (int i = 0; i < serverConfig.requestMessageHandlerCount; i++) {
            threadPool.execute(new RequestProcessor(queue));
        }

        log.info("init request handler success");
    }

    /**
     * close thread pool
     */
    public void close() {
        threadPool.shutdown();
    }

    /**
     * fire a request
     *
     * @param request
     */
    public boolean fire(Request request) {
        try {
            queue.put(request);
        } catch (InterruptedException e) {
            log.warn("put request to queue failed", e);
            return false;
        }
        return true;
    }

    /**
     * handle an request
     *
     * @throws Exception
     */
    public void handle(Request request) throws Exception {
        if (request instanceof CheckInRequest) {
            // 设备登陆签到
            parseCheckInRequest((CheckInRequest) request);
        } else if (request instanceof StartChargeRequest) {
            // 开始充电结果上报
            parseStartChargeRequest((StartChargeRequest) request);
        } else if (request instanceof StopChargeRequest) {
            // 停止充电结果上报
            parseStopChargeRequest((StopChargeRequest) request);
        }
    }

    /**
     * 设备登陆签到
     *
     * @param request
     */
    @Transactional
    public void parseCheckInRequest(CheckInRequest request) {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = simpleDateFormat.format(date);
        CheckInRequest checkInRequest = request;
        String deviceNumber = checkInRequest.getDeviceNumber();
        int connectNetMode = checkInRequest.getConnectNetMode();
        if (connectNetMode != ConstantConfig.DEV_RECONNECT_NET) {
            // 设备非断网重连，需判断是否需要退款
            List<DevicePort> devicePortList = devicePortMapper.findLikeDeviceNumber(deviceNumber);
            if (Objects.nonNull(devicePortList) && !devicePortList.isEmpty()) {
                for (DevicePort devicePort : devicePortList) {
                    // 查找最近一条进行中的订单，判断是否需要退款
                    ChargeOrder chargeOrder = chargeOrderMapper.findByPortIdLast(devicePort.getId());

                    if (Objects.nonNull(chargeOrder)
                            && chargeOrder.getPayStatus() != ConstantConfig.REFUND
                            && chargeOrder.getPayStatus() != ConstantConfig.INVOICED
                            && chargeOrder.getPayStatus() != ConstantConfig.UNREFUND
                            && chargeOrder.getOrderStatus() != ConstantConfig.FINISH_SUCCESS
                            && chargeOrder.getOrderStatus() != ConstantConfig.FINISH_OUT_OF_AC) {
                        long certId = chargeOrder.getCertId();
                        ChargeCert chargeCert = chargeCertMapper.findByCertId(certId);
                        if (Objects.isNull(chargeCert)) {
                            log.warn("[CheckInRequest][{}]cert({}) card not exist", devicePort.getPortNumber(), certId);
                            continue;
                        }

                        if (chargeOrder.getPayment() > 0) {
                            // 使用余额支付
                            boolean refund = false;
                            boolean refundAll = false;
                            int actPayment = 0;

                            if (chargeOrder.getOrderStatus() == ConstantConfig.CREATED || chargeOrder.getOrderStatus() == ConstantConfig.UNCHARGEING) {
                                refund = true;
                                refundAll = true;
                            } else {
                                // 如果实际充电时长小于站点费率设置的最低费率，产生退款
                                String feeDetail = chargeOrder.getFeeDetailSnap();
                                int defaultChargeRateHour = serverConfig.getDefaultChargeRateHour();
                                int actualChargeTime = chargeOrder.getDuration();

                                if (StringUtils.isBlank(feeDetail)) {
                                    if (actualChargeTime < defaultChargeRateHour) {
                                        refund = true;
                                        actPayment = serverConfig.getDefaultChargeRateFee();
                                        log.info("[CheckInRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({})",
                                                deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, defaultChargeRateHour);
                                    }
                                } else {
                                    TypeReference<Map<String, Integer>> type = new TypeReference<Map<String, Integer>>() {
                                    };
                                    Map<String, Integer> rates = (Map<String, Integer>) JacksonUtil.json2Map(feeDetail, type);
                                    if (Objects.isNull(rates) || rates.isEmpty()) {
                                        if (actualChargeTime < defaultChargeRateHour) {
                                            refund = true;
                                            actPayment = serverConfig.getDefaultChargeRateFee();
                                            log.info("[CheckInRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({})",
                                                    deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, defaultChargeRateHour);
                                        }
                                    } else {
                                        LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueAscending(rates);
                                        Map.Entry<String, Integer> minRate = sortRates.entrySet().iterator().next();
                                        if (actualChargeTime < Integer.parseInt(minRate.getKey())) {
                                            refund = true;
                                            actPayment = minRate.getValue();
                                            log.info("[CheckInRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({})",
                                                    deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, minRate.getKey());
                                        }
                                    }
                                }
                            }

                            if (refund) {
                                // 实际退款
                                int payment = chargeOrder.getPayment();//Payment预收金额
                                int refundTotal = payment - actPayment;
                                int orderType = ConstantConfig.ORDER_TYPE_EXCEPTION;//订单类型异常?

                                int paymentAct = chargeOrder.getPaymentAct();//PaymentAct实账支付费用
                                int refundAct = 0;//实账退款
                                int virtualPayment = chargeOrder.getVirtualPayment();//VirtualPayment虚拟账户支付费用
                                int virtualRefund = 0;//virtualRefund虚账退款

                                if (chargeOrder.getPayType() == ConstantConfig.PAY_BY_BALANCE) {//余额支付时
                                    // 根据扣款类型进行退款
                                    if (chargeOrder.getPaySrc() == ConstantConfig.PAY_SRC_REAL) {//扣款来源来自实账的话
                                        refundAct = paymentAct - actPayment;//退款金额=实账支付费用-实际支付费用
                                        paymentAct = actPayment;
                                    } else {//扣款来自虚张
                                        if (refundAll) {//全额退款情况下
                                            if (chargeOrder.getPaySrc() == ConstantConfig.PAY_SRC_VIRTUAL) {//扣款来源来自虚张的话
                                                virtualRefund = virtualPayment;//
                                                virtualPayment = virtualPayment - virtualRefund;
                                            } else {
                                                virtualRefund = virtualPayment;
                                                virtualPayment = 0;
                                                refundAct = paymentAct;
                                                paymentAct = 0;
                                            }
                                        } else {//非全额退款情况下
                                            if (refundTotal <= virtualPayment) {//virtualPayment虚拟账户支付费用
                                                virtualRefund = refundTotal;
                                                virtualPayment = virtualPayment - virtualRefund;
                                            } else {//
                                                paymentAct = paymentAct - (refundTotal - virtualPayment);
                                                virtualPayment = 0;
                                                virtualRefund = refundTotal;
                                            }
                                        }
                                    }
                                } else if (chargeOrder.getPayType() == ConstantConfig.PAY_BY_ALIPAY || chargeOrder.getPayType() == ConstantConfig.PAY_BY_WEIPAY) {
                                    refundAct = paymentAct - actPayment;//实账退款=实账支付-实际支付
                                    paymentAct = actPayment;

                                }

                                if (chargeOrder.getOrderStatus() == ConstantConfig.CREATED || chargeOrder.getOrderStatus() == ConstantConfig.ONGOING) {//订单状态为已创建或创建中
                                    chargeOrder.setFinishedAt(now);//
                                }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                                //异常订单
                                chargeOrder.setChargeFinishReason(ReasonUserCode.DEVICE_RESTART.getDescribe());//获取
                                chargeOrder.refundSetter(chargeOrder.getPayment(), paymentAct, virtualPayment, refundAct, virtualRefund, orderType, ConstantConfig.REFUND, ConstantConfig.FINISH_SUCCESS, now);

                                chargeOrderMapper.refundUpdate(chargeOrder);
                                log.info("[ReportStopChargeRequest][{}]order({}) refund update, refundAct({}), actPayment({})",
                                        deviceNumber, chargeOrder.getOrderNumber(), refundAct, actPayment);
/////////////////////////////////////////////////////////////////////////////////////////////////////
                                // 退款流水
                                /**
                                 * 当支付方式为支付宝或微信时实账余额应为余额,调用微信退款接口将微信退款的金额原路返回
                                 */
                                if (chargeCert.getType() == ConstantConfig.PAY_BY_WEIPAY || chargeCert.getType() == ConstantConfig.PAY_BY_ALIPAY) {
                                    int curValue = chargeCert.getCurValue() + virtualRefund - refundAct;
                                    int curRealValue = chargeCert.getRealValue() - refundAct;//实账余额为
                                    int refundMoney = refundAct - actPayment;//退款金额
                                    RefundRecord refundRecord = new RefundRecord();

                                }
                                int curValue = chargeCert.getCurValue() + virtualRefund;   //CurValue     当前剩余有效次数或者有效时长，或者虚拟账户余额
                                int curRealValue = chargeCert.getRealValue() + refundAct;//
                                ChargeStream chargeStream = new ChargeStream();
                                int operatorSrc = 0;
                                if (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE) {
                                    operatorSrc = ConstantConfig.OPERATOR_SRC_APP;//小程序支付
                                } else {
                                    operatorSrc = ConstantConfig.OPERATOR_SRC_CARD;//卡支付
                                }
                                chargeStream.setter(chargeOrder.getId(), chargeCert.getId(), chargeCert.getUserId(), chargeCert.getBeginedAt(), chargeCert.getFinishedAt(),
                                        ConstantConfig.STREAM_TYPE_REFOUND, refundAct, chargeCert.getCurValue(), virtualRefund, curValue, chargeCert.getRealValue(), refundAct, curRealValue,
                                        operatorSrc, chargeCert.getId().intValue(), ReasonUserCode.DEVICE_RESTART.getDescribe());

                                log.info("chargeStream={}", chargeStream);
                                chargeStreamMapper.insert(chargeStream);
                                log.info("[ReportStopChargeRequest][{}]order({}) refund stream({})",
                                        deviceNumber, chargeOrder.getOrderNumber(), chargeStream.getId());

                                // 更正卡余额
                                chargeCert.setCurValue(curValue);
                                chargeCert.setRealValue(curRealValue);

                                // 更新订单退款流水ID
                                chargeOrder.setRefundStreamId(chargeStream.getId());
                                chargeOrderMapper.updateRefundStream(chargeOrder.getSequenceNumber(), chargeOrder.getRefundStreamId());
                            }

                            chargeCert.setCertStatus(ConstantConfig.CARD_AVAILABLE);
                            chargeCertMapper.updateCertStatus(chargeCert);
                            log.info("[CheckInRequest][{}]update chargeCert({}), status({}), curValue({})",
                                    deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), chargeCert.getCurValue());

                            if (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE) {
                                String result = HttpRequestUtil.notifyUserRequest(chargeOrder.getOrderNumber(), ReasonUserCode.DEVICE_RESTART.getDescribe(), ConstantConfig.NOTIFY_USER_FINISHED, serverConfig.getUserPushUrl());
                                log.info("[CheckInRequest][notifyUserRequest]result: {}", result);
                            }
                        } else if (ConstantConfig.CERT_CART_OF_MONTH_TIME == chargeCert.getType() ||
                                ConstantConfig.CERT_CART_OF_MONTH_COUNT == chargeCert.getType()) {
                            long startedAt = 0L;
                            long finishAt = 0L;
                            if (StringUtils.isNotBlank(chargeOrder.getStartedAt())) {
                                try {
                                    startedAt = simpleDateFormat.parse(chargeOrder.getStartedAt()).getTime();
                                    finishAt = startedAt + chargeOrder.getDuration() * 1000;
                                    chargeOrder.setFinishedAt(simpleDateFormat.format(new Date(finishAt)));
                                } catch (Exception e) {
                                    log.warn("[CheckInRequest][{}]parse order({}) start time failed, ",
                                            deviceNumber, chargeOrder.getOrderNumber(), e.getMessage(), e);
                                }
                            } else {
                                chargeOrder.setFinishedAt(now);
                            }
                            chargeOrder.setChargeFinishReason(ReasonUserCode.DEVICE_RESTART.getDescribe());

                            if (ConstantConfig.CERT_CART_OF_MONTH_TIME == chargeCert.getType()) {
                                chargeOrder.setPayType(ConstantConfig.PAY_BY_MONTHLY_TIME_CARD);
                            } else {
                                chargeOrder.setPayType(ConstantConfig.PAY_BY_MONTHLY_COUNT_CARD);
                            }
                            chargeOrder.setOrderType(ConstantConfig.ORDER_TYPE_EXCEPTION);
                            chargeOrder.setPayStatus(ConstantConfig.PAID);
                            chargeOrder.setPayedOrderAt(now);

                            chargeOrder.setOrderStatus(ConstantConfig.FINISH_SUCCESS);

                            chargeOrderMapper.update(chargeOrder);
                            log.info("[CheckInRequest][{}]update order({}), cert({}), sequenceNumber({}), port({}), duration({})",
                                    deviceNumber, chargeOrder.getOrderNumber(), chargeCert.getId(), chargeOrder.getSequenceNumber(), devicePort.getPortNumber(), chargeOrder.getDuration());

                            if (ConstantConfig.CERT_CART_OF_MONTH_TIME == chargeCert.getType()) {
                                int curValue = 0;
                                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                log.info("[CheckInRequest][{}]order({}) startedAt={}, finishedAt={}",
                                        deviceNumber, chargeOrder.getOrderNumber(), chargeOrder.getStartedAt(), chargeOrder.getFinishedAt());
                                LocalDateTime nowDate = LocalDateTime.now();
                                LocalDateTime startDate = LocalDateTime.parse(chargeOrder.getStartedAt().substring(0, 19), df);
                                LocalDateTime finishDate = LocalDateTime.parse(chargeOrder.getFinishedAt().substring(0, 19), df);

                                if (startDate.toLocalDate().isBefore(nowDate.toLocalDate()) && finishDate.toLocalDate().isBefore(nowDate.toLocalDate())) {
                                    curValue = chargeCert.getCurValue();
                                } else if (startDate.toLocalDate().isBefore(nowDate.toLocalDate()) && !finishDate.toLocalDate().isBefore(nowDate.toLocalDate())) {
                                    long current = System.currentTimeMillis();
                                    long zero = current / (1000 * 3600 * 24) * (1000 * 3600 * 24) - TimeZone.getDefault().getRawOffset();
                                    curValue = chargeCert.getCurValue() - (int) ((finishAt - zero) / 1000) / 60;
                                } else {
                                    curValue = chargeCert.getCurValue() - chargeOrder.getDuration() / 60;
                                }

                                // 更新充电卡信息
                                chargeCert.setCurValue(curValue > 0 ? curValue : 0);
                            }
                            chargeCert.setCertStatus(ConstantConfig.CARD_AVAILABLE);
                            chargeCertMapper.updateCertStatus(chargeCert);
                            log.info("[CheckInRequest][{}]update chargeCert({}), status({}), curValue({})",
                                    deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), chargeCert.getCurValue());
                        }
                    }
                }
            } else {
                log.warn("[CheckInRequest][{}]device({}) not found", deviceNumber, deviceNumber);
            }
        }
    }

    /**
     * 开始充电结果上报
     *
     * @param request
     */
    @Transactional
    public void parseStartChargeRequest(StartChargeRequest request) {
        StartChargeRequest startChargeRequest = request;
        String deviceNumber = startChargeRequest.getDeviceNumber();
        DevicePort devicePort = null;
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = simpleDateFormat.format(date);
        String port = startChargeRequest.getPort();

        //退款对象
        RefundRecord refundRecord = null;

        if (StringUtils.isNotBlank(port)) {
            if (ConstantConfig.PORT_ALL.equals(port) || ConstantConfig.PORT_SINGLE.equals(port)) {
                log.warn("[ReportStartChargeRequest][{}]invalid port({}) number", deviceNumber, port);
                return;
            }
            devicePort = devicePortMapper.findByPortNumber(deviceNumber + "-" + port);
            if (Objects.isNull(devicePort)) {
                log.warn("[ReportStartChargeRequest][{}]device({}) not exist or not available", deviceNumber, port);
                return;
            }
        } else { // 充电端口号为空
            log.warn("[ReportStartChargeRequest][{}]invalid port({}) number", deviceNumber, port);
            return;
        }
        int status = startChargeRequest.getStatus();
        if (status == ConstantConfig.FAILURE) { // 启动失败 全部退款
            String sequenceNumber = SequenceNumberGengerator.getInstance().generate(1000 * (long) startChargeRequest.getTimeStamp(),
                    startChargeRequest.getCertId(), devicePort.getId());

            ChargeOrder chargeOrder = chargeOrderMapper.findBySequenceNumber(sequenceNumber);
            if (Objects.isNull(chargeOrder)) {
                log.warn("[ReportStopChargeRequest][{}]charge order({}) not exist", deviceNumber, sequenceNumber);
                return;
            }

            Long certId = chargeOrder.getCertId();
            ChargeCert chargeCert = chargeCertMapper.findByCertId(certId);
            if (Objects.isNull(chargeCert)) {
                log.warn("[CheckInRequest][{}]cert({}) card not exist", devicePort.getPortNumber(), certId);
                return;
            }

            if (chargeOrder.getPayment() > 0
                    && (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE || chargeCert.getType() == ConstantConfig.CERT_CART_OF_BALANCE)
                    && chargeOrder.getPayStatus() != ConstantConfig.REFUND
                    && chargeOrder.getPayStatus() != ConstantConfig.INVOICED
                    && chargeOrder.getPayStatus() != ConstantConfig.UNREFUND) {
                // 使用余额支付
                boolean refund = true;
                int actPayment = 0;
                if (refund) {
                    // 实际退款
                    int payment = chargeOrder.getPayment();
                    int refundTotal = payment - actPayment;
                    int orderType = ConstantConfig.ORDER_TYPE_EXCEPTION;
                    int paymentAct = chargeOrder.getPaymentAct();
                    int refundAct = 0;
                    int virtualPayment = chargeOrder.getVirtualPayment();
                    int virtualRefund = 0;
                    if (chargeOrder.getPayType() == ConstantConfig.PAY_BY_BALANCE) {
                        // 根据扣款类型进行退款
                        if (chargeOrder.getPaySrc() == ConstantConfig.PAY_SRC_REAL) {
                            refundAct = paymentAct - actPayment;
                            paymentAct = actPayment;
                        } else if (chargeOrder.getPaySrc() == ConstantConfig.PAY_SRC_VIRTUAL) {
                            virtualRefund = virtualPayment;
                            virtualPayment = virtualPayment - virtualRefund;
                        } else {
                            virtualRefund = virtualPayment;
                            virtualPayment = 0;
                            refundAct = paymentAct;
                            paymentAct = 0;
                        }
                    } else if (chargeOrder.getPayType() == ConstantConfig.PAY_BY_ALIPAY || chargeOrder.getPayType() == ConstantConfig.PAY_BY_WEIPAY) {
                        refundAct = refundTotal;
                        paymentAct = 0;
                        //生成退款单号
                        String refundNumber = RefundOrderNumberGenerator.getInstance().generateRefundOrder();
                        refundRecord.refundSetter(refundNumber, chargeOrder.getId(), chargeOrder.getUserId(), refundAct, 0, ConstantConfig.UNREFUND, 0, null);
                        refundRecordMapper.insertRefundRecord(refundRecord);

                        //更改订单信息
                        if (chargeOrder.getOrderStatus() == ConstantConfig.CREATED || chargeOrder.getOrderStatus() == ConstantConfig.ONGOING) {
                            chargeOrder.setFinishedAt(now);
                        }
                        chargeOrder.setChargeFinishReason(ReasonUserCode.CHARGE_FAILURE.getDescribe());
                        chargeOrder.refundSetter(chargeOrder.getPayment(), paymentAct, virtualPayment, refundAct, virtualRefund, orderType, ConstantConfig.UNREFUND, ConstantConfig.FINISH_SUCCESS, now);
                        chargeOrderMapper.refundUpdate(chargeOrder);

                        String result = restTemplate.postForObject("http://frog-wechat-service/refund_apply/wx_apply", null, String.class, refundNumber);


                    }

                    if (chargeOrder.getOrderStatus() == ConstantConfig.CREATED || chargeOrder.getOrderStatus() == ConstantConfig.ONGOING) {
                        chargeOrder.setFinishedAt(now);
                    }
                    chargeOrder.setChargeFinishReason(ReasonUserCode.CHARGE_FAILURE.getDescribe());
                    chargeOrder.refundSetter(chargeOrder.getPayment(), paymentAct, virtualPayment, refundAct, virtualRefund, orderType, ConstantConfig.REFUND, ConstantConfig.FINISH_SUCCESS, now);

                    chargeOrderMapper.refundUpdate(chargeOrder);
                    log.info("[ReportStopChargeRequest][{}]order({}) refund update, refundAct({}), actPayment({})",
                            deviceNumber, chargeOrder.getOrderNumber(), refundAct, actPayment);

                    // 退款流水
                    int curValue = chargeCert.getCurValue() + virtualRefund;
                    int curRealValue = chargeCert.getRealValue() + refundAct;
                    ChargeStream chargeStream = new ChargeStream();
                    int operatorSrc = 0;
                    if (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE) {
                        operatorSrc = ConstantConfig.OPERATOR_SRC_APP;
                    } else {
                        operatorSrc = ConstantConfig.OPERATOR_SRC_CARD;
                    }
                    chargeStream.setter(chargeOrder.getId(), chargeCert.getId(), chargeCert.getUserId(), chargeCert.getBeginedAt(), chargeCert.getFinishedAt(),
                            ConstantConfig.STREAM_TYPE_REFOUND, refundAct, chargeCert.getCurValue(), virtualRefund, curValue, chargeCert.getRealValue(), refundAct, curRealValue,
                            operatorSrc, chargeCert.getId().intValue(), ReasonUserCode.CHARGE_FAILURE.getDescribe());

                    log.info("chargeStream={}", chargeStream);
                    chargeStreamMapper.insert(chargeStream);
                    log.info("[ReportStopChargeRequest][{}]order({}) refund stream({})",
                            deviceNumber, chargeOrder.getOrderNumber(), chargeStream.getId());

                    // 更正卡余额
                    chargeCert.setCurValue(curValue);
                    chargeCert.setRealValue(curRealValue);

                    // 更新订单退款流水ID
                    chargeOrder.setRefundStreamId(chargeStream.getId());
                    chargeOrderMapper.updateRefundStream(chargeOrder.getSequenceNumber(), chargeOrder.getRefundStreamId());
                }

                chargeCert.setCertStatus(ConstantConfig.CARD_AVAILABLE);
                chargeCertMapper.updateCertStatus(chargeCert);
                log.info("[CheckInRequest][{}]update chargeCert({}), status({}), curValue({})",
                        deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), chargeCert.getCurValue());

                if (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE) {
                    String result = HttpRequestUtil.notifyUserRequest(chargeOrder.getOrderNumber(), ReasonUserCode.CHARGE_FAILURE.getDescribe(), ConstantConfig.NOTIFY_USER_FINISHED, serverConfig.getUserPushUrl());
                    log.info("[ReportStartChargeRequest][notifyUserRequest]result: {}", result);
                }
            }
            log.warn("[ReportStartChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
        } else { // 状态值非法
            log.warn("[ReportStartChargeRequest][{}]device({}) invalid start status({})", deviceNumber, port, status);
        }
    }

    /**
     * 停止充电结果上报
     *
     * @param request
     */
    @Transactional
    public void parseStopChargeRequest(StopChargeRequest request) {
        int defaultChargeRateHour = serverConfig.getDefaultChargeRateHour();
        StopChargeRequest stopChargeRequest = request;
        Integer status = stopChargeRequest.getStatus();
        String deviceNumber = stopChargeRequest.getDeviceNumber();
        DevicePort devicePort = null;
        String port = stopChargeRequest.getPort();

        if (StringUtils.isNotBlank(port)) {
            if (ConstantConfig.PORT_ALL.equals(port) || ConstantConfig.PORT_SINGLE.equals(port)) {
                log.warn("[ReportStopChargeRequest][{}]invalid port({}) number", deviceNumber, port);
                return;
            }
            devicePort = devicePortMapper.findByPortNumber(deviceNumber + "-" + port);
            if (Objects.isNull(devicePort)) {
                log.warn("[ReportStopChargeRequest][{}]device({}) not exist or not available", deviceNumber + "-" + port);
                return;
            }
        } else { // 充电端口号为空
            log.warn("[ReportStopChargeRequest][{}]invalid port({}) number", deviceNumber, port);
            return;
        }

        if (status == ConstantConfig.SUCCESS) { // 停止成功
            int certId = stopChargeRequest.getCertId();
            int certType = stopChargeRequest.getCertType();
            String sequenceNumber = SequenceNumberGengerator.getInstance().generate(1000 * (long) stopChargeRequest.getTimeStamp(),
                    certId, devicePort.getId());

            ChargeOrder chargeOrder = chargeOrderMapper.findBySequenceNumber(sequenceNumber);
            if (Objects.isNull(chargeOrder)) {
                log.warn("[ReportStopChargeRequest][{}]charge order({}) not exist", deviceNumber, sequenceNumber);
                return;
            }

            ChargeCert chargeCert = chargeCertMapper.findByCertId(certId);
            if (Objects.isNull(chargeCert)) {
                log.warn("[ReportStopChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
                return;
            } else {
                Date date = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String now = simpleDateFormat.format(date);
                int actualChargeTime = stopChargeRequest.getActualChargeTime();
                int reason = stopChargeRequest.getReason();

                // 判断是否需要退款
                Integer payment = chargeOrder.getPayment();
                if (payment > 0
                        && (certType == ConstantConfig.CERT_CART_OF_BALANCE || certType == ConstantConfig.CERT_OF_PHONE)
                        && chargeOrder.getPayStatus() != ConstantConfig.REFUND
                        && chargeOrder.getPayStatus() != ConstantConfig.INVOICED
                        && chargeOrder.getPayStatus() != ConstantConfig.UNREFUND) {
                    boolean refund = false;
                    boolean refundAll = false;
                    int actPayment = 0;

                    // 充电失败
                    if (reason == ReasonUserCode.CHARGE_FAILURE.getStatusCode()) {
                        refund = true;
                        refundAll = true;
                    } else {
                        // 如果实际充电时长小于站点费率设置的最低费率，产生退款
                        String feeDetail = chargeOrder.getFeeDetailSnap();
                        if (StringUtils.isBlank(feeDetail)) {
                            if (actualChargeTime < defaultChargeRateHour) {
                                refund = true;
                                actPayment = serverConfig.getDefaultChargeRateFee();
                                log.info("[ReportStopChargeRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({})",
                                        deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, defaultChargeRateHour);
                            }
                        } else {
                            TypeReference<Map<String, Integer>> type = new TypeReference<Map<String, Integer>>() {
                            };
                            Map<String, Integer> rates = (Map<String, Integer>) JacksonUtil.json2Map(feeDetail, type);
                            if (Objects.isNull(rates) || rates.isEmpty()) {
                                if (actualChargeTime < defaultChargeRateHour) {
                                    refund = true;
                                    actPayment = serverConfig.getDefaultChargeRateFee();
                                    log.info("[ReportStopChargeRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({}), actPayment({})",
                                            deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, defaultChargeRateHour, actPayment);
                                }
                            } else {
                                LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueAscending(rates);
                                Map.Entry<String, Integer> minRate = sortRates.entrySet().iterator().next();
                                if (actualChargeTime < Integer.parseInt(minRate.getKey())) {
                                    refund = true;
                                    actPayment = minRate.getValue();
                                    log.info("[ReportStopChargeRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({}), actPayment({})",
                                            deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, minRate.getKey(), actPayment);
                                }
                            }
                        }
                    }

                    if (refund) {
                        // 实际退款
                        int refundTotal = payment - actPayment;
                        int orderType = ConstantConfig.ORDER_TYPE_ADJUST;
                        if (reason != ConstantConfig.FINISH_CHARGE_NORMAL
                                && reason != ConstantConfig.FINISH_CHARGE_USER
                                && reason != ConstantConfig.FINISH_CHARGE_AUTO) {
                            orderType = ConstantConfig.ORDER_TYPE_EXCEPTION;
                        }

                        int paymentAct = chargeOrder.getPaymentAct();
                        int refundAct = 0;
                        int virtualPayment = chargeOrder.getVirtualPayment();
                        int virtualRefund = 0;

                        if (chargeOrder.getPayType() == ConstantConfig.PAY_BY_BALANCE) {
                            // 根据扣款类型进行退款
                            if (chargeOrder.getPayType() != ConstantConfig.PAY_BY_ALIPAY || chargeOrder.getPayType() != ConstantConfig.PAY_BY_WEIPAY) {


                                if (chargeOrder.getPaySrc() == ConstantConfig.PAY_SRC_REAL) {
                                    refundAct = paymentAct - actPayment;
                                    paymentAct = actPayment;
                                    log.info("[PAY_SRC_REAL]refundAct({}), paymentAct({})", refundAct, paymentAct);
                                } else {
                                    if (refundAll) {
                                        if (chargeOrder.getPaySrc() == ConstantConfig.PAY_SRC_VIRTUAL) {
                                            virtualRefund = virtualPayment;
                                            virtualPayment = virtualPayment - virtualRefund;
                                        } else {
                                            virtualRefund = virtualPayment;
                                            virtualPayment = 0;
                                            refundAct = paymentAct;
                                            paymentAct = 0;
                                        }
                                    } else {
                                        if (refundTotal <= virtualPayment) {
                                            virtualRefund = refundTotal;
                                            virtualPayment = virtualPayment - virtualRefund;
                                        } else {
                                            paymentAct = paymentAct - (refundTotal - virtualPayment);
                                            virtualPayment = 0;
                                            virtualRefund = refundTotal;
                                        }
                                    }
                                    log.info("[PAY_SRC_VIRTUAL]refundAct({}), paymentAct({}), virtualRefund({}), virtualPayment({})", refundAct, paymentAct, virtualRefund, virtualPayment);
                                }
                            }
                        } else if (chargeOrder.getPayType() == ConstantConfig.PAY_BY_ALIPAY || chargeOrder.getPayType() == ConstantConfig.PAY_BY_WEIPAY) {
                            refundAct = paymentAct - actPayment;
                            paymentAct = actPayment;
                        }

                        if (chargeOrder.getOrderStatus() == ConstantConfig.CREATED || chargeOrder.getOrderStatus() == ConstantConfig.ONGOING) {
                            chargeOrder.setFinishedAt(now);
                        }
                        chargeOrder.refundSetter(payment, paymentAct, virtualPayment, refundAct, virtualRefund, orderType, ConstantConfig.REFUND, ConstantConfig.FINISH_SUCCESS, now);

                        chargeOrderMapper.refundUpdate(chargeOrder);
                        log.info("[ReportStopChargeRequest][{}]order({}) refund update, refundAct({}), actPayment({})",
                                deviceNumber, chargeOrder.getOrderNumber(), refundAct, actPayment);


                        // 退款流水
                        int curValue = chargeCert.getCurValue() + virtualRefund;
                        int curRealValue = chargeCert.getRealValue() + refundAct;
                        ChargeStream chargeStream = new ChargeStream();
                        int operatorSrc = 0;
                        if (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE) {
                            operatorSrc = ConstantConfig.OPERATOR_SRC_APP;
                        } else {
                            operatorSrc = ConstantConfig.OPERATOR_SRC_CARD;
                        }
                        chargeStream.setter(chargeOrder.getId(), chargeCert.getId(), chargeCert.getUserId(), chargeCert.getBeginedAt(), chargeCert.getFinishedAt(),
                                ConstantConfig.STREAM_TYPE_REFOUND, refundAct, chargeCert.getCurValue(), virtualRefund, curValue, chargeCert.getRealValue(), refundAct, curRealValue,
                                operatorSrc, chargeCert.getId().intValue(), ReasonUserCode.getType(reason));

                        log.info("chargeStream={}", chargeStream);
                        chargeStreamMapper.insert(chargeStream);
                        log.info("[ReportStopChargeRequest][{}]order({}) refund stream({})",
                                deviceNumber, chargeOrder.getOrderNumber(), chargeStream.getId());

                        // 更正卡余额
                        chargeCert.setCurValue(curValue);
                        chargeCert.setRealValue(curRealValue);

                        // 设置订单退款流水ID
                        chargeOrder.setRefundStreamId(chargeStream.getId());
                        chargeOrderMapper.updateRefundStream(chargeOrder.getSequenceNumber(), chargeOrder.getRefundStreamId());
                    }
                }
                chargeCert.setCertStatus(ConstantConfig.CARD_AVAILABLE);
                chargeCertMapper.updateCertStatus(chargeCert);
                log.info("[ReportStopChargeRequest][{}]update chargeCert({}), status({}), curValue({}), realValue({})",
                        deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), chargeCert.getCurValue(), chargeCert.getRealValue());
                if (chargeCert.getType() == ConstantConfig.CERT_OF_PHONE) {
                    String result = HttpRequestUtil.notifyUserRequest(chargeOrder.getOrderNumber(), ReasonUserCode.getType(stopChargeRequest.getReason()), ConstantConfig.NOTIFY_USER_FINISHED, serverConfig.getUserPushUrl());
                    log.info("[ReportStopChargeRequest][notifyUserRequest]result: {}", result);
                }
            }
        } else if (status == ConstantConfig.FAILURE) { // 停止失败
            log.warn("[ReportStopChargeRequest][{}]device({}) stop charge failed", deviceNumber, port);
        } else { // 状态值非法
            log.warn("[ReportStopChargeRequest][{}]device({}) invalid stop status({})", deviceNumber, port, status);
        }
    }

}
/**
 *
 */
package com.chargedot.charge.handler;

import com.chargedot.charge.config.ConstantConfig;
import com.chargedot.charge.config.ServerConfig;
import com.chargedot.charge.handler.request.*;
import com.chargedot.charge.mapper.*;
import com.chargedot.charge.message.KafkaProducer;
import com.chargedot.charge.model.*;
import com.chargedot.charge.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author gmm
 *
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
    private CardStreamMapper cardStreamMapper;

    @Autowired
    private KafkaProducer kafkaProducer;

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

        } else if (request instanceof CheckAuthorityRequest) {
            // 刷卡鉴权
            parseCheckAuthorityRequest((CheckAuthorityRequest) request);

        } else if (request instanceof CheckAuthorityExpiredRequest) {
            // 刷卡鉴权
            parseCheckAuthorityRequest((CheckAuthorityExpiredRequest) request,true);

        }else if (request instanceof StartChargeRequest) {
            // 开始充电结果上报
            parseStartChargeRequest((StartChargeRequest) request);

        } else if (request instanceof StopChargeRequest) {
            // 停止充电结果上报
            parseStopChargeRequest((StopChargeRequest) request);
        }
    }

    /**
     * 设备登陆签到
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
            // 设备非断网重连，触发退款
            kafkaProducer.send(ConstantConfig.DW_CHARGE_REFUND_TOPIC, request.getDeviceNumber(), JacksonUtil.bean2Json(checkInRequest));
            List<DevicePort> devicePortList = devicePortMapper.findLikeDeviceNumber(deviceNumber);
            if (Objects.nonNull(devicePortList) && !devicePortList.isEmpty()) {
                for (DevicePort devicePort : devicePortList) {
                    CouplerDynamicDetail detail = new CouplerDynamicDetail();
                    devicePort.setTryOccupyUserId(0);
                    devicePort.setDetail(JacksonUtil.bean2Json(detail));
                    devicePortMapper.update(devicePort);
                    log.info("[ReportStopChargeRequest][{}]update device({}), status({}), occupyUserId({})",
                            deviceNumber, devicePort.getPortNumber(), devicePort.getStatus(), devicePort.getTryOccupyUserId());
                }
            }
        }
    }

    /**
     * 刷卡鉴权(兼容0x11)
     * @param request
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void parseCheckAuthorityRequest(CheckAuthorityRequest request) {
        parseCheckAuthorityRequest(request,false);

    }

    /**
     * 刷卡鉴权(兼容0x11,0x2C)
     * @param request
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void parseCheckAuthorityRequest(CheckAuthorityRequest request,boolean checkExpired) {
        CheckAuthorityRequest checkAuthorityRequest = request;
        String deviceNumber = checkAuthorityRequest.getDeviceNumber();
        String cardNumber = checkAuthorityRequest.getCardNumber();
        String port = checkAuthorityRequest.getPort();
        int seqNumber = request.getSeqNumber();
        boolean authorized = true;
        DevicePort devicePort = null;


        if (ConstantConfig.PORT_ALL.equals(port)) {
            devicePort = devicePortMapper.findLikeDeviceNumberAvailable(deviceNumber);
            if (Objects.isNull(devicePort)) {
                authorized = false;
                log.warn("[CheckAuthorityRequest][{}]device({}) not exist or not available", deviceNumber, port);
            }
        } else {
            String portNumber = deviceNumber + "-" + port;
            devicePort = devicePortMapper.findByPortNumberAvailable(portNumber);
            if (Objects.isNull(devicePort)) {
                authorized = false;
                log.warn("[CheckAuthorityRequest][{}]device({}) state unavailable", deviceNumber, port);
            }
        }

        ChargeCert chargeCert = null;
        long certId = 0L;
        int result = 2;
        int duration = 0;
        int certType = 0; // 卡类型：1包月次卡 2包月包时卡 3充值卡
        int chargeBalance = 0; // 卡余额（次数/时长/余额）
        int expiredDate=0;//卡有效期天数
        int occupyPort=0;//用户是否正占用别的端口，0表示没有占用，其他为占用

        if (authorized) {
            chargeCert = chargeCertMapper.findByCertNumber(cardNumber);
            if (Objects.nonNull(chargeCert)) {
                certId = chargeCert.getId();
                certType = chargeCert.getType();
                //字符串转时间
                String finishedAt = chargeCert.getFinishedAt();
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate finishDate = LocalDate.parse(finishedAt, df);
                LocalDate now = LocalDate.now();
                if (finishDate.isBefore(now)) {
                    // 有效期
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) has expired", deviceNumber, chargeCert.getCertNumber());
                }else{
                    expiredDate= (int) finishDate.until(now, ChronoUnit.DAYS);
                    log.info("[CheckAuthorityRequest]expiredDate=[{}];card({}) ", expiredDate, chargeCert.getCertNumber());
                }

                if (chargeCert.getForbidStatus() == ConstantConfig.CARD_FORBIDDEN) {
                    // 封禁状态
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) not active", deviceNumber, chargeCert.getCertNumber());
                }

                if (chargeCert.getCertStatus() != ConstantConfig.CARD_AVAILABLE) {
                    // 使用状态
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) current state unavailable(status={}) ",
                            deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus());
                }

                if (chargeCert.getType() != ConstantConfig.CARD_TYPE_OF_BALANCE) {
                    if (chargeCert.getCurValue() <= ConstantConfig.CARD_LOWEST_USE_COUNT) {
                        // 剩余时长或剩余次数
                        authorized = false;
                        log.warn("[CheckAuthorityRequest][{}]card({}) remaining time or balance or usage insufficient(curValue={})",
                                deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus());
                    }
                } else {
                    if ((chargeCert.getCurValue() + chargeCert.getRealValue()) <= ConstantConfig.CARD_LOWEST_USE_COUNT) {
                        // 余额
                        authorized = false;
                        log.warn("[CheckAuthorityRequest][{}]card({}) remaining time or balance or usage insufficient(curValue={})",
                                deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus());
                    }
                }


                if (chargeCert.getType() == ConstantConfig.CARD_TYPE_OF_BALANCE) {
                    log.info("[CheckAuthorityRequest][{}]card({}) defaultDetailFee({})",
                            deviceNumber, chargeCert.getCertNumber(), serverConfig.getDefaultFeeDetailSnap());
                    // 余额卡需要判断余额是否满足站点充电费率
                    if (StringUtils.isBlank(devicePort.getFeeDetail())) {
                        // 站点未设置费率，与参数配置的默认费率比较
                        if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < serverConfig.defaultChargeRateFee) {
                            authorized = false;
                            log.warn("[CheckAuthorityRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                    deviceNumber, chargeCert.getCertNumber(), (chargeCert.getCurValue() + chargeCert.getRealValue()), serverConfig.defaultChargeRateFee);
                        } else {
                            // 充电时长等于参数配置的默认时长
                            duration = serverConfig.defaultChargeRateHour / 60;
                        }
                    } else {
                        TypeReference<Map<String, Integer>> type = new TypeReference<Map<String, Integer>>() {
                        };
                        Map<String, Integer> rates = (Map<String, Integer>) JacksonUtil.json2Map(devicePort.getFeeDetail(), type);
                        if (Objects.isNull(rates) || rates.isEmpty()) {
                            if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < serverConfig.defaultChargeRateFee) {
                                authorized = false;
                                log.warn("[CheckAuthorityRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                        deviceNumber, chargeCert.getCertNumber(), (chargeCert.getCurValue() + chargeCert.getRealValue()), serverConfig.defaultChargeRateFee);
                            } else {
                                // 充电时长等于参数配置的默认时长
                                duration = serverConfig.defaultChargeRateHour / 60;
                            }
                        } else {
                            LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueDescending(rates);
                            Map.Entry<String, Integer> maxRate = sortRates.entrySet().iterator().next();
                            if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < maxRate.getValue()) {
                                // 余额小于最大费率，则与最小费率比价
                                Map.Entry<String, Integer> entry = null;
                                try {
                                    entry = getTailByReflection(sortRates);
                                } catch (Exception e) {
                                    log.warn("[CheckAuthorityRequest][{}]feeDetail map sort failed, ", deviceNumber, e.getMessage(), e);
                                    return;
                                }
                                if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < entry.getValue()) {
                                    // 余额小于最小费率
                                    authorized = false;
                                    log.warn("[CheckAuthorityRequest][{}]card({}) insufficient balance(curValue={}), station minimum rate({})",
                                            deviceNumber, chargeCert.getCertNumber(), (chargeCert.getCurValue() + chargeCert.getRealValue()), entry.getValue());
                                } else {
                                    duration = Integer.parseInt(entry.getKey()) / 60;
                                    log.info("[CheckAuthorityRequest][{}]card({}) preset duration({}) of station minimum rate(duration={}, fee={})",
                                            deviceNumber, chargeCert.getCertNumber(), duration, entry.getKey(), entry.getValue());
                                }
                            } else {
                                duration = Integer.parseInt(maxRate.getKey()) / 60;
                                log.info("[CheckAuthorityRequest][{}]card({}) preset duration({}) of station maximum rate(duration={}, fee={})",
                                        deviceNumber, chargeCert.getCertNumber(), duration, maxRate.getKey(), maxRate.getValue());
                            }
                        }
                    }
                }

                // 用户知否占用其他设备
                devicePort = devicePortMapper.findByOccupyUserId((int) certId);
                if (Objects.nonNull(devicePort)) {
                    authorized = false;
                    occupyPort=1;
                    log.warn("[CheckAuthorityRequest][{}]card({}) are occupying device({})",
                            deviceNumber, cardNumber, devicePort.getPortNumber());
                }
            } else {
                // 卡片不存在
                authorized = false;
                log.warn("[CheckAuthorityRequest][{}]card({}) not exist", deviceNumber, cardNumber);
            }
        }

        int sequenceNumber = (int) (System.currentTimeMillis() / 1000);

        Map<String, Object> params = new HashMap<>();
        if (authorized) {
            result = 0;
            chargeBalance = (chargeCert.getCurValue() + chargeCert.getRealValue());
            if (chargeCert.getType() != ConstantConfig.CARD_TYPE_OF_BALANCE) {
                duration = chargeCert.getCurValue();
            }
            log.info("[CheckAuthorityRequest][{}]card({}) authentication success, presetChargeTime({})", deviceNumber, cardNumber, duration);
        } else {
            certId = 0L;
            log.info("[CheckAuthorityRequest][{}]card({}) authentication failed", deviceNumber, cardNumber);
        }

        if(checkExpired){
            params.put("OperationType", "DCheckAuthorityExpiredRequest");
        }else {
            params.put("OperationType", "DCheckAuthorityRequest");
        }
        params.put("Result", result);
        params.put("Duration", duration);
        params.put("SequenceNumber", sequenceNumber);
        params.put("UserId", certId);
        params.put("Port", port);
        params.put("CardType", certType);
        params.put("ChargeBalance", chargeBalance);
        params.put("SeqNumber", seqNumber);
        params.put("ExpiredDate",expiredDate);
        params.put("OccupyPort",occupyPort);
        kafkaProducer.send(ConstantConfig.S2D_RES_TOPIC, deviceNumber, JacksonUtil.map2Json(params));
    }

    /**
     * 开始充电结果或者用户扫码解锁响应结果上报
     * @param request
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Transactional
    public void parseStartChargeRequest(StartChargeRequest request) throws NoSuchFieldException, IllegalAccessException {
        StartChargeRequest startChargeRequest = request;
        String deviceNumber = startChargeRequest.getDeviceNumber();
        DevicePort devicePort = null;
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = simpleDateFormat.format(date);
        String port = startChargeRequest.getPort();

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

        Integer status = startChargeRequest.getStatus();
        if (status == 1 && request.getType() == 1) { // 上报开始充电结果,启动成功
            Integer certId = startChargeRequest.getCertId();
            Integer certType = startChargeRequest.getCertType();
            if (certType != ConstantConfig.CERT_OF_PHONE) { // 卡用户
                ChargeCert chargeCert = chargeCertMapper.findByCertId(startChargeRequest.getCertId());
                if (Objects.isNull(chargeCert)) { // 卡不存在
                    log.warn("[ReportStartChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
                } else { // 创建卡充电订单
                    String sequenceNumber = SequenceNumberGengerator.getInstance().generate(1000 * (long) startChargeRequest.getTimeStamp(),
                            certId, devicePort.getId());

                    ChargeOrder chargeOrder = chargeOrderMapper.findBySequenceNumber(sequenceNumber);
                    if (Objects.nonNull(chargeOrder)) {
                        log.warn("[ReportStartChargeRequest][{}]charge order({}) exist", deviceNumber, sequenceNumber);
                        return;
                    }

                    String orderNumber = ChargeOrderNumberGenerator.getInstance().generate(devicePort.getDeviceId());
                    chargeOrder = new ChargeOrder();
                    chargeOrder.startSetter(devicePort.getId(), devicePort.getDeviceId(), devicePort.getStationId(), certId, chargeCert.getUserId(),
                            chargeCert.getCertNumber(), sequenceNumber, orderNumber, startChargeRequest.getPresetChargeTime() * 60);

                    if (certType == ConstantConfig.CARD_TYPE_OF_MONTH_COUNT || certType == ConstantConfig.CARD_TYPE_OF_MONTH_TIME) {
                        int payType = ConstantConfig.PAY_NULL;
                        chargeOrder.paySetter(0, 0, 0, 0, ConstantConfig.UNPAID,
                                payType, devicePort.getFeeDetail(), null);
                        chargeOrderMapper.insert(chargeOrder);
                        log.info("[ReportStartChargeRequest][{}]create new order({}), card({}), user({}), sequenceNumber({}), port({})",
                                deviceNumber, orderNumber, chargeCert.getCertNumber(), certId, sequenceNumber, port);

                        // 更新充电卡信息
                        chargeCert.setCertStatus(ConstantConfig.CARD_OCCUPYING);
                        chargeCertMapper.updateCertStatus(chargeCert);
                        log.info("[ReportStartChargeRequest][{}]update chargeCard({}), status({}), curValue({})",
                                deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), chargeCert.getCurValue());
                    } else {
                        int payment = 0;
                        String feeDetailSnap = serverConfig.defaultFeeDetailSnap;
                        if (StringUtils.isBlank(devicePort.getFeeDetail())) {
                            // 站点未设置费率，与参数配置的默认费率比较
                            if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < serverConfig.defaultChargeRateFee) {
                                log.warn("[ReportStartChargeRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                        deviceNumber, chargeCert.getCertNumber(), (chargeCert.getCurValue() + chargeCert.getRealValue()), serverConfig.defaultChargeRateFee);
                            }
                            payment = serverConfig.defaultChargeRateFee;
                        } else {
                            TypeReference<Map<String, Integer>> type = new TypeReference<Map<String, Integer>>() {
                            };
                            Map<String, Integer> rates = (Map<String, Integer>) JacksonUtil.json2Map(devicePort.getFeeDetail(), type);
                            if (Objects.isNull(rates) || rates.isEmpty()) {
                                if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < serverConfig.defaultChargeRateFee) {
                                    log.warn("[ReportStartChargeRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                            deviceNumber, chargeCert.getCertNumber(), (chargeCert.getCurValue() + chargeCert.getRealValue()), serverConfig.defaultChargeRateFee);
                                }
                                payment = serverConfig.defaultChargeRateFee;
                            } else {
                                LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueDescending(rates);
                                Map.Entry<String, Integer> maxRate = sortRates.entrySet().iterator().next();
                                if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < maxRate.getValue()) {
                                    // 余额小于最大费率，则与最小费率比价
                                    Map.Entry<String, Integer> entry = getTailByReflection(sortRates);
                                    if ((chargeCert.getCurValue() + chargeCert.getRealValue()) < entry.getValue()) {
                                        // 余额小于最小费率
                                        log.warn("[ReportStartChargeRequest][{}]card({}) insufficient balance(curValue={}), station minimum rate({})",
                                                deviceNumber, chargeCert.getCertNumber(), (chargeCert.getCurValue() + chargeCert.getRealValue()), entry.getValue());
                                    }
                                    payment = entry.getValue();
                                } else {
                                    payment = maxRate.getValue();
                                    log.info("[ReportStartChargeRequest][{}]card({}) preset duration({}) of station maximum rate(duration={}, fee={})",
                                            deviceNumber, chargeCert.getCertNumber(), startChargeRequest.getPresetChargeTime(),
                                            maxRate.getKey(), maxRate.getValue());
                                }
                                feeDetailSnap = devicePort.getFeeDetail();
                            }
                        }

                        int paymentAct = 0;
                        int virtualPayment = 0;
                        int paySrc = 0;
                        // 虚实帐分离
                        if (payment <= chargeCert.getRealValue()) {
                            paymentAct = payment;
                            paySrc = ConstantConfig.PAY_SRC_REAL;
                        } else if (chargeCert.getRealValue() == 0) {
                            virtualPayment = payment;
                            paySrc = ConstantConfig.PAY_SRC_VIRTUAL;
                        } else {
                            paymentAct = chargeCert.getRealValue();
                            virtualPayment = payment - paymentAct;
                            paySrc = ConstantConfig.PAY_SRC_REAL_AND_VIRTUAL;
                        }

                        chargeOrder.paySetter(payment, paymentAct, virtualPayment, paySrc, ConstantConfig.PAID,
                                ConstantConfig.PAY_BY_BALANCE, feeDetailSnap, now);

                        // 创建订单
                        chargeOrderMapper.insert(chargeOrder);
                        log.info("[ReportStartChargeRequest][{}]create new order({}), status({}), card({}), user({}), payment({}), sequenceNumber({}), port({})",
                                deviceNumber, orderNumber, chargeOrder.getOrderStatus(), chargeCert.getCertNumber(), chargeCert.getUserId(), payment, sequenceNumber, port);

                        // 扣余额
                        chargeCert.setCertStatus(ConstantConfig.CARD_OCCUPYING);
                        int curValue = chargeCert.getCurValue();
                        int realValue = chargeCert.getRealValue();
                        chargeCert.setCurValue(curValue - virtualPayment);
                        chargeCert.setRealValue(realValue - paymentAct);
                        chargeCertMapper.updateCertStatus(chargeCert);
                        log.info("[ReportStartChargeRequest][{}]update chargeCard({}), status({}), before pay balance(real={}, virtual={}), now balance(real={}, virtual={})",
                                deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), realValue, curValue, chargeCert.getRealValue(), chargeCert.getCurValue());

                        // 支付流水
                        CardStream cardStream = new CardStream();
                        cardStream.setter(chargeOrder.getId(), chargeCert.getId(), chargeCert.getUserId(), chargeCert.getBeginedAt(), chargeCert.getFinishedAt(),
                                ConstantConfig.STREAM_TYPE_PAY, curValue, virtualPayment, chargeCert.getCurValue(), realValue, paymentAct, chargeCert.getRealValue(), ConstantConfig.OPERATOR_SRC_CARD, certId);
                        cardStreamMapper.insert(cardStream);
                        log.info("[ReportStartChargeRequest][{}]order({}) payment stream({})", deviceNumber, orderNumber, cardStream.getId());

                        // 设置订单支付流水ID
                        chargeOrder.setPayStreamId(cardStream.getId());
                        chargeOrderMapper.updatePayStream(chargeOrder.getSequenceNumber(), chargeOrder.getPayStreamId());

                    }

                    // 更新端口信息
                    CouplerDynamicDetail detail = new CouplerDynamicDetail();
                    detail.setter(certId, sequenceNumber, orderNumber, chargeCert.getCertNumber());
                    devicePort.setTryOccupyUserId(certId);
                    devicePort.setStatus(ConstantConfig.CHARGING);
                    devicePort.setDetail(JacksonUtil.bean2Json(detail));
                    devicePortMapper.update(devicePort);
                    log.info("[ReportStartChargeRequest][{}] update device({}), status({}), occupyUserId({})",
                            deviceNumber, port, devicePort.getStatus(), devicePort.getTryOccupyUserId());
                }
            }
        } else if (status == 2) { // 启动失败
            kafkaProducer.send(ConstantConfig.DW_CHARGE_REFUND_TOPIC, request.getDeviceNumber(), JacksonUtil.bean2Json(startChargeRequest));
            log.warn("[ReportStartChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
        } else { // 状态值非法
            log.warn("[ReportStartChargeRequest][{}]device({}) invalid start status({})", deviceNumber, port, status);
        }
    }

    /**
     * 停止充电结果上报
     * @param request
     */
    @Transactional
    public void parseStopChargeRequest(StopChargeRequest request) {
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

        if (status == 1) { // 停止成功
            Integer certId = stopChargeRequest.getCertId();
            Integer certType = stopChargeRequest.getCertType();
            String sequenceNumber = SequenceNumberGengerator.getInstance().generate(1000 * (long) stopChargeRequest.getTimeStamp(),
                    certId, devicePort.getId());

            ChargeOrder chargeOrder = chargeOrderMapper.findBySequenceNumber(sequenceNumber);
            if (Objects.isNull(chargeOrder)) {
                log.warn("[ReportStopChargeRequest][{}]charge order({}) not exist", deviceNumber, sequenceNumber);
                return;
            }
            if (!chargeOrder.isOnGoing()) {
                log.info("[ReportStopChargeRequest][{}]discard the order({}), order({}) status is {}",
                        deviceNumber, sequenceNumber, chargeOrder.getOrderNumber(), chargeOrder.getOrderStatus());
                return;
            }

            ChargeCert chargeCert = chargeCertMapper.findByCertId(certId);
            if (Objects.isNull(chargeCert)) { // 充电凭证不存在
                log.warn("[ReportStopChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
                return;
            } else { // 更新充电订单
                Date date = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String now = simpleDateFormat.format(date);
                Integer actualChargeTime = stopChargeRequest.getActualChargeTime();

                chargeOrder.setDuration(actualChargeTime);
                chargeOrder.setFinishedAt(now);
                Integer reason = stopChargeRequest.getReason();
                chargeOrder.setChargeFinishReason(ReasonCode.getType(reason));

                if (certType == ConstantConfig.CARD_TYPE_OF_MONTH_COUNT || certType == ConstantConfig.CARD_TYPE_OF_MONTH_TIME) {
                    if (certType == ConstantConfig.CARD_TYPE_OF_MONTH_TIME) {
                        chargeOrder.setPayType(ConstantConfig.PAY_BY_MONTHLY_TIME_CARD);
                    } else {
                        chargeOrder.setPayType(ConstantConfig.PAY_BY_MONTHLY_COUNT_CARD);
                    }
                    chargeOrder.setPayStatus(ConstantConfig.PAID);
                    chargeOrder.setPayedOrderAt(now);
                }

                chargeOrder.setOrderStatus(ConstantConfig.FINISH_SUCCESS);

                chargeOrderMapper.update(chargeOrder);
                log.info("[ReportStopChargeRequest][{}]update order({}), user({}), sequenceNumber({}), port({}), duration({})",
                        deviceNumber, chargeOrder.getOrderNumber(), certId, sequenceNumber, port, chargeOrder.getDuration());

                // 包时月卡 包时次卡需要扣费
                if (ConstantConfig.CARD_TYPE_OF_MONTH_COUNT == chargeCert.getType() ||
                        ConstantConfig.CARD_TYPE_OF_MONTH_TIME == chargeCert.getType()) {
                    long startedAt = 0L;
                    long finishAt = 0L;
                    if (StringUtils.isNotBlank(chargeOrder.getStartedAt())) {
                        try {
                            startedAt = simpleDateFormat.parse(chargeOrder.getStartedAt()).getTime();
                            finishAt = startedAt + chargeOrder.getDuration() * 1000;
                        } catch (Exception e) {
                            log.warn("[CheckInRequest][{}]parse order({}) start time failed, ",
                                    deviceNumber, chargeOrder.getOrderNumber(), e.getMessage(), e);
                        }
                    }

                    if (ConstantConfig.CARD_TYPE_OF_MONTH_TIME == chargeCert.getType()) {
                        int curValue = 0;
                        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        log.info("[ReportStopChargeRequest][{}]order({}) startedAt={}, finishedAt={}",
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
                    log.info("[ReportStopChargeRequest][{}]update chargeCard({}), status({}), curValue({})",
                            deviceNumber, chargeCert.getCertNumber(), chargeCert.getCertStatus(), chargeCert.getCurValue());
                }

                // 更新端口信息
                CouplerDynamicDetail detail = new CouplerDynamicDetail();
                devicePort.setTryOccupyUserId(0);
                if (reason == 2) {
                    devicePort.setStatus(5);
                }
                devicePort.setStatus(2);
                devicePort.setDetail(JacksonUtil.bean2Json(detail));
                devicePortMapper.update(devicePort);
                log.info("[ReportStopChargeRequest][{}]update device({}), status({}), occupyUserId({})",
                        deviceNumber, port, devicePort.getStatus(), devicePort.getTryOccupyUserId());
            }

            // 判断是否需要退款
            if (stopChargeRequest.getReason() != ReasonCode.SUCCESS.getStatusCode()
                    && stopChargeRequest.getReason() != ReasonCode.FULL_STOP.getStatusCode()
                    && stopChargeRequest.getReason() != ReasonCode.USER_STOP.getStatusCode()) {
                kafkaProducer.send(ConstantConfig.DW_CHARGE_REFUND_TOPIC, request.getDeviceNumber(), JacksonUtil.bean2Json(stopChargeRequest));
            } else {
                String result = HttpRequestUtil.notifyUserRequest(chargeOrder.getOrderNumber(), ReasonUserCode.getType(stopChargeRequest.getReason()), ConstantConfig.NOTIFY_USER_FINISHED, serverConfig.getUserPushUrl());
                log.info("[ReportStopChargeRequest][notifyUserRequest]result: {}", result);
            }
        } else if (status == 2) { // 停止失败
            log.warn("[ReportStopChargeRequest][{}]device({}) stop charge failed", deviceNumber, port);
        } else { // 状态值非法
            log.warn("[ReportStopChargeRequest][{}]device({}) invalid stop status({})", deviceNumber, port, status);
        }
    }

    public <K, V> Map.Entry<K, V> getTailByReflection(LinkedHashMap<K, V> map)
            throws NoSuchFieldException, IllegalAccessException {
        Field tail = map.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Map.Entry<K, V>) tail.get(map);
    }

}
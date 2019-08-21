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
import com.chargedot.charge.util.ChargeOrderNumberGenerator;
import com.chargedot.charge.util.JacksonUtil;
import com.chargedot.charge.util.MapUtil;
import com.chargedot.charge.util.SequenceNumberGengerator;
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
    private ChargeCardMapper chargeCardMapper;

    @Autowired
    private DevicePortMapper devicePortMapper;

    @Autowired
    private UserMapper userMapper;

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

                    if (Objects.nonNull(chargeOrder)) {
                        if (chargeOrder.getStartType() == ConstantConfig.START_BY_CARD) {
                            int userId = chargeOrder.getUserId();
                            ChargeCard chargeCard = chargeCardMapper.findByUserId(userId);
                            if (Objects.isNull(chargeCard)) {
                                log.warn("[CheckInRequest][{}]user({}) card not exist", devicePort.getPortNumber(), userId);
                                continue;
                            }

                            if (chargeOrder.getPayment() > 0 && chargeOrder.getPayType() == ConstantConfig.PAY_BY_BALANCE) {
                                // 使用余额支付，启动方式是卡订单进行退款
                                boolean refund = false;
                                int actPayment = 0;

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

                                if (refund) {
                                    // 实际退款
                                    int refundAct = chargeOrder.getPayment() - actPayment;
                                    chargeOrder.refundSetter(actPayment, refundAct, ConstantConfig.ORDER_TYPE_EXCEPTION,
                                            ConstantConfig.REFUND, ConstantConfig.FINISH_SUCCESS, now);

                                    long startedAt = 0L;
                                    if (StringUtils.isNotBlank(chargeOrder.getStartedAt())) {
                                        try {
                                            startedAt = simpleDateFormat.parse(chargeOrder.getStartedAt()).getTime();
                                            long finishAt = startedAt + chargeOrder.getDuration() * 1000;
                                            chargeOrder.setFinishedAt(simpleDateFormat.format(new Date(finishAt)));
                                        } catch (Exception e) {
                                            log.warn("[CheckInRequest][{}]parse order({}) start time failed, ",
                                                    deviceNumber, chargeOrder.getOrderNumber(), e.getMessage(), e);
                                        }
                                    } else {
                                        chargeOrder.setFinishedAt(now);
                                    }
                                    chargeOrder.setChargeFinishReason(ReasonCode.getType(ConstantConfig.DEV_RESTART_MODE));

                                    chargeOrderMapper.refundUpdate(chargeOrder);
                                    log.info("[CheckInRequest][{}]order({}) refund update, refundAct({}), actPayment({})",
                                            deviceNumber, chargeOrder.getOrderNumber(), refundAct, actPayment);

                                    // 退款流水
                                    int curValue = chargeCard.getCurValue();
                                    CardStream cardStream = new CardStream();
                                    cardStream.setter(chargeOrder.getId(), chargeCard.getId(), userId, chargeCard.getBeginedAt(), chargeCard.getFinishedAt(),
                                            ConstantConfig.STREAM_TYPE_REFOUND, curValue, refundAct, ConstantConfig.OPERATOR_SRC_CARD, userId);

                                    cardStreamMapper.insert(cardStream);
                                    log.info("[CheckInRequest][{}]order({}) refund stream({})",
                                            deviceNumber, chargeOrder.getOrderNumber(), cardStream.getId());

                                    // 更正卡余额
                                    chargeCard.setCurValue(curValue + refundAct);

                                    // 更新订单退款流水ID
                                    chargeOrder.setRefundStreamId(cardStream.getId());
                                    chargeOrderMapper.updateRefundStream(chargeOrder.getSequenceNumber(), chargeOrder.getRefundStreamId());
                                }

                                chargeCard.setCardStatus(ConstantConfig.CARD_AVAILABLE);
                                chargeCardMapper.updateCardStatus(chargeCard);
                                log.info("[CheckInRequest][{}]update chargeCard({}), status({}), curValue({})",
                                        deviceNumber, chargeCard.getCardNumber(), chargeCard.getCardStatus(), chargeCard.getCurValue());
                            } else if (ConstantConfig.CARD_TYPE_OF_MONTHLY_TIME == chargeCard.getType() ||
                                    ConstantConfig.CARD_TYPE_OF_MONTH_COUNT == chargeCard.getType()){
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
                                chargeOrder.setChargeFinishReason(ReasonCode.getType(ConstantConfig.DEV_RESTART_MODE));

                                if (ConstantConfig.CARD_TYPE_OF_MONTHLY_TIME == chargeCard.getType()) {
                                    chargeOrder.setPayType(ConstantConfig.PAY_BY_MONTHLY_TIME_CARD);
                                } else {
                                    chargeOrder.setPayType(ConstantConfig.PAY_BY_MONTHLY_COUNT_CARD);
                                }
                                chargeOrder.setOrderType(ConstantConfig.ORDER_TYPE_EXCEPTION);
                                chargeOrder.setPayStatus(ConstantConfig.PAID);
                                chargeOrder.setPayedOrderAt(now);

                                chargeOrder.setOrderStatus(ConstantConfig.FINISH_SUCCESS);

                                chargeOrderMapper.update(chargeOrder);
                                log.info("[CheckInRequest][{}]update order({}), user({}), sequenceNumber({}), port({}), duration({})",
                                        deviceNumber, chargeOrder.getOrderNumber(), userId, chargeOrder.getSequenceNumber(), devicePort.getPortNumber(), chargeOrder.getDuration());

                                int curValue = 0;
                                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                log.info("[CheckInRequest][{}]order({}) startedAt={}, finishedAt={}",
                                        deviceNumber, chargeOrder.getOrderNumber(), chargeOrder.getStartedAt(), chargeOrder.getFinishedAt());
                                LocalDateTime nowDate = LocalDateTime.now();
                                LocalDateTime startDate = LocalDateTime.parse(chargeOrder.getStartedAt().substring(0, 19), df);
                                LocalDateTime finishDate = LocalDateTime.parse(chargeOrder.getFinishedAt().substring(0, 19), df);

                                if (startDate.toLocalDate().isBefore(nowDate.toLocalDate()) && finishDate.toLocalDate().isBefore(nowDate.toLocalDate())) {
                                    curValue = chargeCard.getCurValue();
                                } else if (startDate.toLocalDate().isBefore(nowDate.toLocalDate()) && !finishDate.toLocalDate().isBefore(nowDate.toLocalDate())) {
                                    long current = System.currentTimeMillis();
                                    long zero = current/(1000*3600*24)*(1000*3600*24) - TimeZone.getDefault().getRawOffset();
                                    curValue = chargeCard.getCurValue() - (int)((finishAt - zero) / 1000) / 60;
                                } else {
                                    curValue = chargeCard.getCurValue() - chargeOrder.getDuration() / 60;
                                }

                                // 更新充电卡信息
                                chargeCard.setCurValue(curValue > 0 ? curValue : 0);
                                chargeCard.setCardStatus(ConstantConfig.CARD_AVAILABLE);
                                chargeCardMapper.updateCardStatus(chargeCard);
                                log.info("[CheckInRequest][{}]update chargeCard({}), status({}), curValue({})",
                                        deviceNumber, chargeCard.getCardNumber(), chargeCard.getCardStatus(), chargeCard.getCurValue());
                            }

                        }
                    }
                }
            } else {
                log.warn("[CheckInRequest][{}]device({}) not found", deviceNumber, deviceNumber);
            }
        }
    }


    /**
     * 刷卡鉴权
     * @param request
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void parseCheckAuthorityRequest(CheckAuthorityRequest request) {
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

        ChargeCard chargeCard = null;
        Integer userId = 0;
        int result = 2;
        int duration = 0;
        int cardType = 0; // 卡类型：1包月次卡 2包月包时卡 3充值卡
        int chargeBalance = 0; // 卡余额（次数/时长/余额）
        if (authorized) {
            chargeCard = chargeCardMapper.findByCardNumber(cardNumber);
            if (Objects.nonNull(chargeCard)) {
                userId = chargeCard.getUserId();
                cardType = chargeCard.getType();
                //字符串转时间
                String finishedAt = chargeCard.getFinishedAt();
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate finishDate = LocalDate.parse(finishedAt, df);
                LocalDate now = LocalDate.now();
                if (finishDate.isBefore(now)) {
                    // 有效期
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) has expired", deviceNumber, chargeCard.getCardNumber());
                }

                if (chargeCard.getForbidStatus() == ConstantConfig.CARD_FORBIDDEN) {
                    // 封禁状态
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) not active", deviceNumber, chargeCard.getCardNumber());
                }

                if (chargeCard.getCardStatus() != ConstantConfig.CARD_AVAILABLE) {
                    // 使用状态
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) current state unavailable(status={}) ",
                            deviceNumber, chargeCard.getCardNumber(), chargeCard.getCardStatus());
                }

                if (chargeCard.getCurValue() <= ConstantConfig.CARD_LOWEST_USE_COUNT) {
                    // 余额或剩余时长或剩余次数
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]card({}) remaining time or balance or usage insufficient(curValue={})",
                            deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue());
                }

                if (chargeCard.getType() == ConstantConfig.CARD_TYPE_OF_BALANCE) {
                    log.info("[CheckAuthorityRequest][{}]card({}) defaultDetailFee({})",
                            deviceNumber, chargeCard.getCardNumber(), serverConfig.getDefaultFeeDetailSnap());
                    // 余额卡需要判断余额是否满足站点充电费率
                    if (StringUtils.isBlank(devicePort.getFeeDetail())) {
                        // 站点未设置费率，与参数配置的默认费率比较
                        if (chargeCard.getCurValue() < serverConfig.defaultChargeRateFee) {
                            authorized = false;
                            log.warn("[CheckAuthorityRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                    deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue(), serverConfig.defaultChargeRateFee);
                        } else {
                            // 充电时长等于参数配置的默认时长
                            duration = serverConfig.defaultChargeRateHour / 60;
                        }
                    } else {
                        TypeReference<Map<String, Integer>> type = new TypeReference<Map<String, Integer>>() {
                        };
                        Map<String, Integer> rates = (Map<String, Integer>) JacksonUtil.json2Map(devicePort.getFeeDetail(), type);
                        if (Objects.isNull(rates) || rates.isEmpty()) {
                            if (chargeCard.getCurValue() < serverConfig.defaultChargeRateFee) {
                                authorized = false;
                                log.warn("[CheckAuthorityRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                        deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue(), serverConfig.defaultChargeRateFee);
                            } else {
                                // 充电时长等于参数配置的默认时长
                                duration = serverConfig.defaultChargeRateHour / 60;
                            }
                        } else {
                            LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueDescending(rates);
                            Map.Entry<String, Integer> maxRate = sortRates.entrySet().iterator().next();
                            if (chargeCard.getCurValue() < maxRate.getValue()) {
                                // 余额小于最大费率，则与最小费率比价
                                Map.Entry<String, Integer> entry = null;
                                try {
                                    entry = getTailByReflection(sortRates);
                                } catch (Exception e) {
                                    log.warn("[CheckAuthorityRequest][{}]feeDetail map sort failed, ", deviceNumber, e.getMessage(), e);
                                    return;
                                }
                                if (chargeCard.getCurValue() < entry.getValue()) {
                                    // 余额小于最小费率
                                    authorized = false;
                                    log.warn("[CheckAuthorityRequest][{}]card({}) insufficient balance(curValue={}), station minimum rate({})",
                                            deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue(), entry.getValue());
                                } else {
                                    duration = Integer.parseInt(entry.getKey()) / 60;
                                    log.info("[CheckAuthorityRequest][{}]card({}) preset duration({}) of station minimum rate(duration={}, fee={})",
                                            deviceNumber, chargeCard.getCardNumber(), duration, entry.getKey(), entry.getValue());
                                }
                            } else {
                                duration = Integer.parseInt(maxRate.getKey()) / 60;
                                log.info("[CheckAuthorityRequest][{}]card({}) preset duration({}) of station maximum rate(duration={}, fee={})",
                                        deviceNumber, chargeCard.getCardNumber(), duration, maxRate.getKey(), maxRate.getValue());
                            }
                        }
                    }
                }

                User user = userMapper.findById(userId);
                if (Objects.isNull(user)) { // 用户不存在
                    authorized = false;
                    log.warn("[CheckAuthorityRequest][{}]user({}) not exist or status is forbidden", deviceNumber, port, userId);
                } else { // 用户知否占用其他设备
                    devicePort = devicePortMapper.findByOccupyUserId(userId);
                    if (Objects.nonNull(devicePort)) {
                        authorized = false;
                        log.warn("[CheckAuthorityRequest][{}]card({}) associated user({}), are occupying device({})",
                                deviceNumber, cardNumber, userId, devicePort.getPortNumber());
                    }
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
            chargeBalance = chargeCard.getCurValue();
            if (chargeCard.getType() != ConstantConfig.CARD_TYPE_OF_BALANCE) {
                duration = chargeCard.getCurValue();
            }
            log.info("[CheckAuthorityRequest][{}]card({}) authentication success, presetChargeTime({})", deviceNumber, cardNumber, duration);
        } else {
            userId = 0;
            log.info("[CheckAuthorityRequest][{}]card({}) authentication failed", deviceNumber, cardNumber);
        }

        params.put("OperationType", "DCheckAuthorityRequest");
        params.put("Result", result);
        params.put("Duration", duration);
        params.put("SequenceNumber", sequenceNumber);
        params.put("UserId", userId);
        params.put("Port", port);
        params.put("CardType", cardType);
        params.put("ChargeBalance", chargeBalance);
        params.put("SeqNumber", seqNumber);
        kafkaProducer.send(ConstantConfig.S2D_RES_TOPIC, deviceNumber, JacksonUtil.map2Json(params));
    }

    /**
     * 开始充电结果上报
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
        if (status == 1) { // 启动成功
            Integer userId = startChargeRequest.getUserId();
            Integer userType = startChargeRequest.getUserType();
            if (userType == ConstantConfig.USER_TYPE_MONTH_COUNT
                    || userType == ConstantConfig.USER_TYPE_MONTH_TIME
                    || userType == ConstantConfig.USER_TYPE_PREPAID_CARD) { // 卡用户
                ChargeCard chargeCard = chargeCardMapper.findByUserId(startChargeRequest.getUserId());
                if (Objects.isNull(chargeCard)) { // 卡不存在
                    log.warn("[ReportStartChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
                } else { // 创建卡充电订单
                    String sequenceNumber = SequenceNumberGengerator.getInstance().generate(1000 * (long) startChargeRequest.getTimeStamp(),
                            userId, devicePort.getId());

                    ChargeOrder chargeOrder = chargeOrderMapper.findBySequenceNumber(sequenceNumber);
                    if (Objects.nonNull(chargeOrder)) {
                        log.warn("[ReportStartChargeRequest][{}]charge order({}) exist", deviceNumber, sequenceNumber);
                        return;
                    }

                    String orderNumber = ChargeOrderNumberGenerator.getInstance().generate(devicePort.getDeviceId());
                    chargeOrder = new ChargeOrder();
                    chargeOrder.startSetter(devicePort.getId(), devicePort.getDeviceId(), devicePort.getStationId(), userId,
                            chargeCard.getCardNumber(), sequenceNumber, orderNumber, startChargeRequest.getPresetChargeTime() * 60);

                    if (userType == ConstantConfig.USER_TYPE_MONTH_COUNT || userType == ConstantConfig.USER_TYPE_MONTH_TIME) {
                        chargeOrder.paySetter(0, 0, ConstantConfig.UNPAID, ConstantConfig.ONGOING,
                                ConstantConfig.PAY_NULL, devicePort.getFeeDetail(), null);
                        chargeOrderMapper.insert(chargeOrder);
                        log.info("[ReportStartChargeRequest][{}]create new order({}), card({}), user({}), sequenceNumber({}), port({})",
                                deviceNumber, orderNumber, chargeCard.getCardNumber(), userId, sequenceNumber, port);

                        // 更新充电卡信息
                        chargeCard.setCardStatus(ConstantConfig.CARD_OCCUPYING);
                        chargeCardMapper.updateCardStatus(chargeCard);
                        log.info("[ReportStartChargeRequest][{}]update chargeCard({}), status({}), curValue({})",
                                deviceNumber, chargeCard.getCardNumber(), chargeCard.getCardStatus(), chargeCard.getCurValue());
                    } else {
                        int payment = 0;
                        String feeDetailSnap = serverConfig.defaultFeeDetailSnap;
                        if (StringUtils.isBlank(devicePort.getFeeDetail())) {
                            // 站点未设置费率，与参数配置的默认费率比较
                            if (chargeCard.getCurValue() < serverConfig.defaultChargeRateFee) {
                                log.warn("[ReportStartChargeRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                        deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue(), serverConfig.defaultChargeRateFee);
                            }
                            payment = serverConfig.defaultChargeRateFee;
                        } else {
                            TypeReference<Map<String, Integer>> type = new TypeReference<Map<String, Integer>>() {
                            };
                            Map<String, Integer> rates = (Map<String, Integer>) JacksonUtil.json2Map(devicePort.getFeeDetail(), type);
                            if (Objects.isNull(rates) || rates.isEmpty()) {
                                if (chargeCard.getCurValue() < serverConfig.defaultChargeRateFee) {
                                    log.warn("[ReportStartChargeRequest][{}]card({}) insufficient balance(curValue={}), station default minimum rate({})",
                                            deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue(), serverConfig.defaultChargeRateFee);
                                }
                                payment = serverConfig.defaultChargeRateFee;
                            } else {
                                LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueDescending(rates);
                                Map.Entry<String, Integer> maxRate = sortRates.entrySet().iterator().next();
                                if (chargeCard.getCurValue() < maxRate.getValue()) {
                                    // 余额小于最大费率，则与最小费率比价
                                    Map.Entry<String, Integer> entry = getTailByReflection(sortRates);
                                    if (chargeCard.getCurValue() < entry.getValue()) {
                                        // 余额小于最小费率
                                        log.warn("[ReportStartChargeRequest][{}]card({}) insufficient balance(curValue={}), station minimum rate({})",
                                                deviceNumber, chargeCard.getCardNumber(), chargeCard.getCurValue(), entry.getValue());
                                    }
                                    payment = entry.getValue();
                                } else {
                                    payment = maxRate.getValue();
                                    log.info("[ReportStartChargeRequest][{}]card({}) preset duration({}) of station maximum rate(duration={}, fee={})",
                                            deviceNumber, chargeCard.getCardNumber(), startChargeRequest.getPresetChargeTime(),
                                            maxRate.getKey(), maxRate.getValue());
                                }
                                feeDetailSnap = devicePort.getFeeDetail();
                            }
                        }

                        chargeOrder.paySetter(payment, payment, ConstantConfig.PAID, ConstantConfig.ONGOING,
                                ConstantConfig.PAY_BY_BALANCE, feeDetailSnap, now);

                        // 创建订单
                        chargeOrderMapper.insert(chargeOrder);
                        log.info("[ReportStartChargeRequest][{}]create new order({}), card({}), user({}), payment({}), sequenceNumber({}), port({})",
                                deviceNumber, orderNumber, chargeCard.getCardNumber(), userId, payment, sequenceNumber, port);

                        // 扣余额
                        chargeCard.setCardStatus(ConstantConfig.CARD_OCCUPYING);
                        int curValue = chargeCard.getCurValue();
                        chargeCard.setCurValue(curValue - payment);
                        chargeCardMapper.updateCardStatus(chargeCard);
                        log.info("[ReportStartChargeRequest][{}]update chargeCard({}), status({}), before pay balance={}, now balance={}",
                                deviceNumber, chargeCard.getCardNumber(), chargeCard.getCardStatus(), curValue, chargeCard.getCurValue());

                        // 支付流水
                        CardStream cardStream = new CardStream();
                        cardStream.setter(chargeOrder.getId(), chargeCard.getId(), userId, chargeCard.getBeginedAt(), chargeCard.getFinishedAt(),
                                ConstantConfig.STREAM_TYPE_PAY, curValue, payment, ConstantConfig.OPERATOR_SRC_CARD, userId);
                        cardStreamMapper.insert(cardStream);
                        log.info("[ReportStartChargeRequest][{}]order({}) payment stream({})", deviceNumber, orderNumber, cardStream.getId());

                        // 设置订单支付流水ID
                        chargeOrder.setPayStreamId(cardStream.getId());
                        chargeOrderMapper.updatePayStream(chargeOrder.getSequenceNumber(), chargeOrder.getPayStreamId());

                    }

                    // 更新端口信息
                    CouplerDynamicDetail detail = new CouplerDynamicDetail();
                    detail.setter(userId, sequenceNumber, orderNumber, chargeCard.getCardNumber());
                    devicePort.setTryOccupyUserId(userId);
                    devicePort.setStatus(ConstantConfig.CHARGING);
                    devicePort.setDetail(JacksonUtil.bean2Json(detail));
                    devicePortMapper.update(devicePort);
                    log.info("[ReportStartChargeRequest][{}] update device({}), status({}), occupyUserId({})",
                            deviceNumber, port, devicePort.getStatus(), devicePort.getTryOccupyUserId());
                }
            }
        } else if (status == 2) { // 启动失败
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

        if (status == 1) { // 停止成功
            Integer userId = stopChargeRequest.getUserId();
            Integer userType = stopChargeRequest.getUserType();
            if (userType == ConstantConfig.USER_TYPE_PREPAID_CARD
                    || userType == ConstantConfig.USER_TYPE_MONTH_COUNT
                    || userType == ConstantConfig.USER_TYPE_MONTH_TIME) { // 卡用户
                String sequenceNumber = SequenceNumberGengerator.getInstance().generate(1000 * (long) stopChargeRequest.getTimeStamp(),
                        userId, devicePort.getId());

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

                ChargeCard chargeCard = chargeCardMapper.findByUserId(userId);
                if (Objects.isNull(chargeCard)) { // 卡不存在
                    log.warn("[ReportStopChargeRequest][{}]device({}) start charge failed", deviceNumber, port);
                } else { // 更新充电订单
                    Date date = new Date();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String now = simpleDateFormat.format(date);
                    Integer actualChargeTime = stopChargeRequest.getActualChargeTime();

                    chargeOrder.setDuration(actualChargeTime);
                    chargeOrder.setFinishedAt(now);
                    Integer reason = stopChargeRequest.getReason();
                    chargeOrder.setChargeFinishReason(ReasonCode.getType(reason));

                    if (userType == ConstantConfig.USER_TYPE_MONTH_COUNT || userType == ConstantConfig.USER_TYPE_MONTH_TIME) {
                        if (userType == ConstantConfig.USER_TYPE_MONTH_TIME) {
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
                            deviceNumber, chargeOrder.getOrderNumber(), userId, sequenceNumber, port, chargeOrder.getDuration());

                    // 判断是否需要退款
                    if (userType == ConstantConfig.USER_TYPE_PREPAID_CARD && chargeOrder.getPayment() > 0) {
                        boolean refund = false;
                        int actPayment = 0;

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
                                    log.info("[ReportStopChargeRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({})",
                                            deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, defaultChargeRateHour);
                                }
                            } else {
                                LinkedHashMap<String, Integer> sortRates = (LinkedHashMap<String, Integer>) MapUtil.sortByValueAscending(rates);
                                Map.Entry<String, Integer> minRate = sortRates.entrySet().iterator().next();
                                if (actualChargeTime < Integer.parseInt(minRate.getKey())) {
                                    refund = true;
                                    actPayment = minRate.getValue();
                                    log.info("[ReportStopChargeRequest][{}]order({}) need refund, actChargeTime({}), minRateTime({})",
                                            deviceNumber, chargeOrder.getOrderNumber(), actualChargeTime, minRate.getKey());
                                }
                            }
                        }

                        if (refund) {
                            // 实际退款
                            int refundAct = chargeOrder.getPayment() - actPayment;
                            int orderType = ConstantConfig.ORDER_TYPE_ADJUST;
                            if (reason != ConstantConfig.FINISH_CHARGE_NORMAL
                                    && reason != ConstantConfig.FINISH_CHARGE_USER
                                    && reason != ConstantConfig.FINISH_CHARGE_AUTO) {
                                orderType = ConstantConfig.ORDER_TYPE_EXCEPTION;
                            }
                            chargeOrder.refundSetter(actPayment, refundAct, orderType, ConstantConfig.REFUND, ConstantConfig.FINISH_SUCCESS, now);

                            chargeOrderMapper.refundUpdate(chargeOrder);
                            log.info("[ReportStopChargeRequest][{}]order({}) refund update, refundAct({}), actPayment({})",
                                    deviceNumber, chargeOrder.getOrderNumber(), refundAct, actPayment);

                            // 退款流水
                            int curValue = chargeCard.getCurValue();
                            CardStream cardStream = new CardStream();
                            cardStream.setter(chargeOrder.getId(), chargeCard.getId(), userId, chargeCard.getBeginedAt(), chargeCard.getFinishedAt(),
                                    ConstantConfig.STREAM_TYPE_REFOUND, curValue, refundAct, ConstantConfig.OPERATOR_SRC_CARD, userId);

                            cardStreamMapper.insert(cardStream);
                            log.info("[ReportStopChargeRequest][{}]order({}) refund stream({})",
                                    deviceNumber, chargeOrder.getOrderNumber(), cardStream.getId());

                            // 更正卡余额
                            chargeCard.setCurValue(curValue + refundAct);

                            // 设置订单退款流水ID
                            chargeOrder.setRefundStreamId(cardStream.getId());
                            chargeOrderMapper.updateRefundStream(chargeOrder.getSequenceNumber(), chargeOrder.getRefundStreamId());
                        }
                    }

                    if (userType == ConstantConfig.USER_TYPE_MONTH_COUNT || userType == ConstantConfig.USER_TYPE_MONTH_TIME) {
                        int curValue = 0;
                        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        log.info("[ReportStopChargeRequest][{}]order({}) startedAt={}, finishedAt={}",
                                deviceNumber, chargeOrder.getOrderNumber(), chargeOrder.getStartedAt(), chargeOrder.getFinishedAt());
                        LocalDateTime dateTime = LocalDateTime.parse(chargeOrder.getStartedAt().substring(0, 19), df);
                        LocalDateTime current = LocalDateTime.parse(chargeOrder.getFinishedAt().substring(0, 19), df);
                        if (dateTime.toLocalDate().isBefore(current.toLocalDate())) {
                            curValue = chargeCard.getCurValue() - (current.getHour() * 60 + current.getMinute());
                        } else {
                            curValue = chargeCard.getCurValue() - actualChargeTime / 60;
                        }
                        // 更新充电卡信息
                        chargeCard.setCurValue(curValue > 0 ? curValue : 0);
                    }
                    chargeCard.setCardStatus(ConstantConfig.CARD_AVAILABLE);
                    chargeCardMapper.updateCardStatus(chargeCard);
                    log.info("[ReportStopChargeRequest][{}]update chargeCard({}), status({}), curValue({})",
                            deviceNumber, chargeCard.getCardNumber(), chargeCard.getCardStatus(), chargeCard.getCurValue());

                    // 更新端口信息
                    CouplerDynamicDetail detail = new CouplerDynamicDetail();
                    devicePort.setTryOccupyUserId(0);
                    devicePort.setStatus(ConstantConfig.AVAILABLE);
                    devicePort.setDetail(JacksonUtil.bean2Json(detail));
                    devicePortMapper.update(devicePort);
                    log.info("[ReportStopChargeRequest][{}]update device({}), status({}), occupyUserId({})",
                            deviceNumber, port, devicePort.getStatus(), devicePort.getTryOccupyUserId());
                }
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
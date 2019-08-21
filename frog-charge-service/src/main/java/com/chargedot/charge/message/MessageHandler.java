package com.chargedot.charge.message;

import com.chargedot.charge.config.ConstantConfig;
import com.chargedot.charge.config.CustomConfig;
import com.chargedot.charge.handler.RequestHandler;
import com.chargedot.charge.handler.request.CheckAuthorityRequest;
import com.chargedot.charge.handler.request.CheckInRequest;
import com.chargedot.charge.handler.request.StartChargeRequest;
import com.chargedot.charge.handler.request.StopChargeRequest;
import com.chargedot.charge.mapper.UserMapper;
import com.chargedot.charge.model.ReasonCode;
import com.chargedot.charge.model.User;
import com.chargedot.charge.util.HttpRequestUtil;
import com.chargedot.charge.util.JacksonUtil;
import com.chargedot.charge.util.SpringBeanUtil;
import com.chargedot.charge.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/31
 */
@Slf4j
public class MessageHandler implements Runnable {

    private String key;

    private String message;

    private RestTemplate restTemplate;

    private CustomConfig config;

    public MessageHandler(String key, String message, RestTemplate restTemplate, CustomConfig config) {
        this.key = key;
        this.message = message;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @Override
    public void run() {

        Map mapParam = JacksonUtil.json2Bean(message, Map.class);
        String operationType = (String) mapParam.get("OperationType");

        switch (operationType) {
            case ConstantConfig.StartStopResultPushRequest:
                try {
                    log.info("[StartStopResultPushRequest][{}][message]{}", key, message);
                    //操作类型，1-开始充电结果上报，2-停止充电结果上报，3-用户扫码解锁响应结果上报，4-用户手动结束充电响应结果上报
                    int type = (int) mapParam.get("Type");

                    //设备枪端口号，单枪为"00",多枪设备枪口号一次为0x1A-0xFE对应的十六进制字符串
                    String port = (String) mapParam.get("Port");
                    //启停状态，1-成功，2-失败
                    int status = (int) mapParam.get("Status");
                    //具体原因，0-正常，1-过流停充，2-设备故障停充，3-充电失败，4-用户主动停止，5-充满自动停止，18-无权限，19-拒绝服务，80-系统错误，81-执行失败
                    int reason = (int) mapParam.get("Reason");
                    int userId = (int) mapParam.get("UserId");
                    //充电流水号对应时间戳（秒数值）
                    int timeStamp = (int) mapParam.get("TimeStamp");


                    int presetChargeTime = 0;
                    if (mapParam.containsKey("PresetChargeTime")) {
                        //预设充电时长.单位分钟
                        presetChargeTime = (int) mapParam.get("PresetChargeTime");
                    }

                    int actualChargeTime = 0;
                    if (mapParam.containsKey("ActualChargeTime")) {
                        //实际充电时长.单位s
                        actualChargeTime = (int) mapParam.get("ActualChargeTime");
                    }

                    //refundFlag, 1-故障， 2-正常
                    UserMapper userMapper = SpringBeanUtil.getBean(UserMapper.class);
                    User user = userMapper.findById(userId);
                    Integer userType = 0;
                    if (Objects.nonNull(user)) {
                        userType = user.getType();
                    }
                    if (type == 1) {
                        StartChargeRequest startChargeRequest = new StartChargeRequest();
                        startChargeRequest.setter(key, port, status, reason, userId, timeStamp, userType);
                        startChargeRequest.setPresetChargeTime(presetChargeTime);
                        SpringBeanUtil.getBean(RequestHandler.class).fire(startChargeRequest);
                    } else if (type == 2) {
                        StopChargeRequest stopChargeRequest = new StopChargeRequest();
                        stopChargeRequest.setter(key, port, status, reason, userId, timeStamp, userType);
                        stopChargeRequest.setActualChargeTime(actualChargeTime);
                        stopChargeRequest.setPresetChargeTime(presetChargeTime);
                        SpringBeanUtil.getBean(RequestHandler.class).fire(stopChargeRequest);
                    }
                    if (Objects.nonNull(user)) {
                        userType = user.getType();
                        if (ConstantConfig.USER_TYPE_APP == userType || ConstantConfig.USER_TYPE_BIND_APP == userType) {
                            if (2 == status) {
                                if (1 == type || 3 == type) {
                                    String refundResult = HttpRequestUtil.sendRefundRequest(key, port, userId, timeStamp, config, 1);
                                    log.info("[StartStopResultPushRequest][result]{}", StringUtil.unicodeToString(refundResult));
                                    JacksonUtil.json2Bean(StringUtil.unicodeToString(refundResult), Map.class);
                                    //通知用户启动失败结果
                                    String interfaceName = ConstantConfig.NOTIFY_USER_FINISHED;
                                    String notifyResult = HttpRequestUtil.notifyUserRequest(key, port, userId, timeStamp, ReasonCode.getType(reason), interfaceName, config);
                                    log.info("[StartStopResultPushRequest][result]{}", StringUtil.unicodeToString(notifyResult));
                                }
                            }
                            //开始充电结果上报，通知用户充电成功开始
                            if (1 == status) {
                                if (2 == type) {
                                    if (1 == reason || 2 == reason || 3 == reason) {
                                        String refundResult = HttpRequestUtil.sendRefundRequest(key, port, userId, timeStamp, config, 1);
                                        log.info("[StartStopResultPushRequest][result]{}", StringUtil.unicodeToString(refundResult));
                                        Map resultMap = JacksonUtil.json2Bean(StringUtil.unicodeToString(refundResult), Map.class);
                                        //通知用户启动失败结果
                                        String interfaceName = ConstantConfig.NOTIFY_USER_FINISHED;
                                        String notifyResult = HttpRequestUtil.notifyUserRequest(key, port, userId, timeStamp, ReasonCode.getType(reason), interfaceName, config);
                                        log.info("[StartStopResultPushRequest][result]{}", StringUtil.unicodeToString(notifyResult));
                                    } else {
                                        if (presetChargeTime >= Integer.parseInt(config.getPresetChargeTime())) {
                                            if (actualChargeTime < Integer.parseInt(config.getActualChargeTime()) * 60) {
                                                String refundResult = HttpRequestUtil.sendRefundRequest(key, port, userId, timeStamp, config, 2);
                                                log.info("[StartStopResultPushRequest][result]{}", StringUtil.unicodeToString(refundResult));
                                            }
                                        }
                                        //通知用户成功停止的充电结果
                                        String interfaceName = ConstantConfig.NOTIFY_USER_FINISHED;
                                        String notifyResult = HttpRequestUtil.notifyUserRequest(key, port, userId, timeStamp, ReasonCode.getType(reason), interfaceName, config);
                                        log.info("[StartStopResultPushRequest][result]{}", StringUtil.unicodeToString(notifyResult));
                                    }
                                }
                            }

                        }
                    } else {
                        log.warn("[ReportStartChargeRequest][{}]user({}) not exist or status is forbidden", key, userId);
                    }

                } catch (Exception e) {
                    log.warn("[StartStopResultPushRequest]exception happened ", e);
                }
                break;
            case ConstantConfig.DCheckInRequest:
                log.info("[CheckInRequest][{}][message]{}", key, message);
                if (!mapParam.containsKey("ConnectNetMode")) {
                    log.info("[CheckInRequest]invalid connectNetMode");
                    return;
                }
                //充电授权策略标识，1-通用授权（平台授权，在线桩离线使用鉴权），2-离线鉴权，3-即插即用
                int authorType = (int) mapParam.get("AuthorType");
                //重连标识，1-断网重连，2-断电重连，3-远程重启 4-远程升级重连 5-设备自保护重启
                int connectNetMode = (int) mapParam.get("ConnectNetMode");
                CheckInRequest checkInRequest = new CheckInRequest();
                checkInRequest.setDeviceNumber(key);
                checkInRequest.setAuthorType(authorType);
                checkInRequest.setConnectNetMode(connectNetMode);
                SpringBeanUtil.getBean(RequestHandler.class).fire(checkInRequest);

                if (1 != connectNetMode) {
                    String result = HttpRequestUtil.reconnectRefund(key, config, connectNetMode);
                    log.info("[CheckInRequest][result]{}", StringUtil.unicodeToString(result));
                }

                break;
            case ConstantConfig.DCheckAuthorityRequest:
                try {
                    log.info("[DCheckAuthorityRequest][{}][message]{}", key, message);
                    String cardNumber = (String) mapParam.get("CardNumber");
                    String port = (String) mapParam.get("Port");

                    if (StringUtils.isBlank(cardNumber)) {
                        log.warn("[DCheckAuthorityRequest][{}]invalid cardNumber({})", key, cardNumber);
                        break;
                    }

                    if (StringUtils.isBlank(port)) {
                        log.warn("[DCheckAuthorityRequest][{}]invalid portNumber({})", key, port);
                        break;
                    }

                    if (Objects.isNull(mapParam.get("SeqNumber"))) {
                        log.warn("[DCheckAuthorityRequest][{}]invalid portNumber({})", key, port);
                        break;
                    }
                    int seqNumber = (int) mapParam.get("SeqNumber");

                    CheckAuthorityRequest checkAuthorityRequest = new CheckAuthorityRequest();
                    checkAuthorityRequest.setCardNumber(cardNumber);
                    checkAuthorityRequest.setDeviceNumber(key);
                    checkAuthorityRequest.setPort(port);
                    checkAuthorityRequest.setSeqNumber(seqNumber);
                    SpringBeanUtil.getBean(RequestHandler.class).fire(checkAuthorityRequest);
                } catch (Exception e) {
                    log.warn("[DCheckAuthorityRequest]exception happened ", e);
                }
                break;
            default:
                log.info("[MessageHandler][{}]unsupported operationType({})", key, operationType);
        }

    }


}

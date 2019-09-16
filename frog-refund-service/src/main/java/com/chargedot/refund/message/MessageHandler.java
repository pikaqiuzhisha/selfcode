package com.chargedot.refund.message;

import com.chargedot.refund.config.ConstantConfig;
import com.chargedot.refund.handler.RequestHandler;
import com.chargedot.refund.handler.request.CheckInRequest;
import com.chargedot.refund.handler.request.StartChargeRequest;
import com.chargedot.refund.handler.request.StopChargeRequest;
import com.chargedot.refund.util.JacksonUtil;
import com.chargedot.refund.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/31
 */
@Slf4j
public class MessageHandler implements Runnable {

    private String key;

    private String message;

    public MessageHandler(String key, String message) {
        this.key = key;
        this.message = message;
    }

    @Override
    public void run() {
        Map mapParam = JacksonUtil.json2Bean(message, Map.class);

        String operationType = (String) mapParam.get("OperationType");
        switch (operationType) {
            case ConstantConfig.StartChargeRequest:
                log.info("[StartChargeRequest][{}][message]{}", key, message);
                try {
                    //操作类型，1-开始充电结果上报，2-停止充电结果上报，3-用户扫码解锁响应结果上报，4-用户手动结束充电响应结果上报
                    int type = (int) mapParam.get("Type");

                    //设备枪端口号，单枪为"00",多枪设备枪口号一次为0x1A-0xFE对应的十六进制字符串
                    String port = (String) mapParam.get("Port");

                    //启停状态，1-成功，2-失败
                    int status = (int) mapParam.get("Status");

                    //具体原因，0-正常，1-过流停充，2-设备故障停充，3-充电失败，4-用户主动停止，5-充满自动停止，18-无权限，19-拒绝服务，80-系统错误，81-执行失败
                    int reason = (int) mapParam.get("Reason");

                    // 当前以凭证ID代替userId
                    int certId = (int) mapParam.get("CertId");

                    //充电流水号对应时间戳（秒数值）
                    int timeStamp = (int) mapParam.get("TimeStamp");


                    int presetChargeTime = 0;
                    if (mapParam.containsKey("PresetChargeTime")) {
                        //预设充电时长.单位分钟
                        presetChargeTime = (int) mapParam.get("PresetChargeTime");
                    }

                    int certType = (int) mapParam.get("CertType");

                    StartChargeRequest startChargeRequest = new StartChargeRequest();
                    startChargeRequest.setOperationType(operationType);
                    startChargeRequest.setter(key, port, status, reason, certId, timeStamp, certType);
                    startChargeRequest.setPresetChargeTime(presetChargeTime);
                    SpringBeanUtil.getBean(RequestHandler.class).fire(startChargeRequest);
                } catch (Exception e) {
                    log.warn("[StartChargeRequest]exception happened ", e);
                }
                break;
            case ConstantConfig.StopChargeRequest:
                log.info("[StopChargeRequest][{}][message]{}", key, message);
                try {
                    //操作类型，1-开始充电结果上报，2-停止充电结果上报，3-用户扫码解锁响应结果上报，4-用户手动结束充电响应结果上报
                    int type = (int) mapParam.get("Type");

                    //设备枪端口号，单枪为"00",多枪设备枪口号一次为0x1A-0xFE对应的十六进制字符串
                    String port = (String) mapParam.get("Port");

                    //启停状态，1-成功，2-失败
                    int status = (int) mapParam.get("Status");

                    //具体原因，0-正常，1-过流停充，2-设备故障停充，3-充电失败，4-用户主动停止，5-充满自动停止，18-无权限，19-拒绝服务，80-系统错误，81-执行失败
                    int reason = (int) mapParam.get("Reason");

                    // 当前以凭证ID代替userId
                    int userId = (int) mapParam.get("CertId");

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

                    int certType = (int) mapParam.get("CertType");

                    StopChargeRequest stopChargeRequest = new StopChargeRequest();
                    stopChargeRequest.setOperationType(operationType);
                    stopChargeRequest.setter(key, port, status, reason, userId, timeStamp, certType);
                    stopChargeRequest.setActualChargeTime(actualChargeTime);
                    stopChargeRequest.setPresetChargeTime(presetChargeTime);
                    SpringBeanUtil.getBean(RequestHandler.class).fire(stopChargeRequest);
                } catch (Exception e) {
                    log.warn("[StartChargeRequest]exception happened ", e);
                }
                break;
            case ConstantConfig.DCheckInRequest:
                log.info("[CheckInRequest][{}][message]{}", key, message);
                try {
                    if (!mapParam.containsKey("ConnectNetMode")) {
                        log.info("[CheckInRequest]invalid connectNetMode");
                        return;
                    }
                    //充电授权策略标识，1-通用授权（平台授权，在线桩离线使用鉴权），2-离线鉴权，3-即插即用
                    int authorType = (int) mapParam.get("AuthorType");

                    //重连标识，1-断网重连，2-断电重连，3-远程重启 4-远程升级重连 5-设备自保护重启
                    int connectNetMode = (int) mapParam.get("ConnectNetMode");

                    CheckInRequest checkInRequest = new CheckInRequest();
                    checkInRequest.setOperationType(operationType);
                    checkInRequest.setDeviceNumber(key);
                    checkInRequest.setAuthorType(authorType);
                    checkInRequest.setConnectNetMode(connectNetMode);
                    SpringBeanUtil.getBean(RequestHandler.class).fire(checkInRequest);
                } catch (Exception e) {
                    log.warn("[StartChargeRequest]exception happened ", e);
                }
                break;
            default:
                log.info("[MessageHandler][{}]unsupported operationType({})", key, operationType);
        }
    }
}

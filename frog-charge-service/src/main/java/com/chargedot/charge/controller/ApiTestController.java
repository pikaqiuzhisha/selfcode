package com.chargedot.charge.controller;

import com.chargedot.charge.config.ConstantConfig;
import com.chargedot.charge.controller.vo.CommonResult;
import com.chargedot.charge.util.JacksonUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/6/8
 */
@Slf4j
@Controller
@RequestMapping("/v1")
public class ApiTestController {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @HystrixCommand(fallbackMethod = "defaultSendMessage")
    @RequestMapping(value = "/send_message", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> reportDeviceFault(@RequestBody Map<String, Object> params) {
        log.info("[send_message][params]{}", JacksonUtil.bean2Json(params));
        try {
            if (params.containsKey("OperationType")) {
                String operationType = (String) params.get("OperationType");
                switch (operationType) {
                    case ConstantConfig.DReportDeviceFaultRequest:
                        if (params.containsKey("DeviceNumber")
                                && params.containsKey("DeviceId")
                                && params.containsKey("PortId")
                                && params.containsKey("DeviceType")
                                && params.containsKey("StationId")
                                && params.containsKey("FaultFlag")
                                && params.containsKey("FaultType")
                                && params.containsKey("FaultCode")
                                && params.containsKey("Port")) {
                            params.put("Timestamp", System.currentTimeMillis() / 1000);
                            log.info("[topic]{}", ConstantConfig.DWD_ERROR_REQ);
                            log.info("[deviceNumber]{}", params.get("DeviceNumber"));
                            log.info("[message]{}", JacksonUtil.bean2Json(params));
                            kafkaTemplate.send(ConstantConfig.DWD_ERROR_REQ, params.get("DeviceNumber"), JacksonUtil.bean2Json(params));
                        } else {
                            return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
                        }
                        break;
                    case ConstantConfig.StartStopResultPushRequest:
                        if (params.containsKey("DeviceNumber")
                                && params.containsKey("Type")
                                && params.containsKey("Port")
                                && params.containsKey("Status")
                                && params.containsKey("Reason")
                                && params.containsKey("UserId")
                                ) {
                            params.put("TimeStamp", System.currentTimeMillis() / 1000);
                            log.info("[topic]{}", ConstantConfig.DWD_START_STOP_RESULT_REQ);
                            log.info("[deviceNumber]{}", params.get("DeviceNumber"));
                            log.info("[message]{}", JacksonUtil.bean2Json(params));
                            kafkaTemplate.send(ConstantConfig.DWD_START_STOP_RESULT_REQ, params.get("DeviceNumber"), JacksonUtil.bean2Json(params));
                        } else {
                            return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
                        }
                        break;
                    case ConstantConfig.DCheckInRequest:
                        if (params.containsKey("DeviceNumber")
                                && params.containsKey("DeviceType")
                                && params.containsKey("ProtocolVersion")
                                && params.containsKey("SoftVersion")
                                && params.containsKey("SoftModel")
                                && params.containsKey("AuthorType")
                                && params.containsKey("DeviceLockType")
                                && params.containsKey("ConnectNetMode")) {
                            log.info("[topic]{}", ConstantConfig.DWD_CHECK_IN_REQ);
                            log.info("[deviceNumber]{}", params.get("DeviceNumber"));
                            log.info("[message]{}", JacksonUtil.bean2Json(params));
                            kafkaTemplate.send(ConstantConfig.DWD_CHECK_IN_REQ, params.get("DeviceNumber"), JacksonUtil.bean2Json(params));
                        } else {
                            return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
                        }
                        break;
                    case "PUpgradeRequest":
                        if (params.containsKey("DeviceNumber")
                                && params.containsKey("Provider")
                                && params.containsKey("Version")
                                && params.containsKey("PacketLen")
                                && params.containsKey("Checksum")
                                && params.containsKey("UpBytes")
                                && params.containsKey("UpMode")
                                && params.containsKey("DownPath")
                                && params.containsKey("StorePath")) {
                            log.info("[topic]DWS-UPGRADE-REQ");
                            log.info("[deviceNumber]{}", params.get("DeviceNumber"));
                            log.info("[message]{}", JacksonUtil.bean2Json(params));
                            kafkaTemplate.send("DWS-UPGRADE-REQ", params.get("DeviceNumber"), JacksonUtil.bean2Json(params));
                        } else {
                            return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
                        }
                        break;
                }
            } else {
                return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.info("exception happened ", e);
            return new ResponseEntity<CommonResult>(CommonResult.buildResult(500), HttpStatus.OK);
        }
        return new ResponseEntity<>(CommonResult.buildSuccessResult("send success"), HttpStatus.OK);
    }

    public ResponseEntity<CommonResult> defaultSendMessage(@RequestBody Map<String, Object> params) {
        log.info("[defaultSendMessage][param]{}", JacksonUtil.bean2Json(params));
        return new ResponseEntity<CommonResult>(CommonResult.buildResult(-1), HttpStatus.OK);
    }


}

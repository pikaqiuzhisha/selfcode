package com.chargedot.charge.message;

import com.chargedot.charge.config.ConstantConfig;
import com.chargedot.charge.exception.InsertException;
import com.chargedot.charge.exception.UpdateException;
import com.chargedot.charge.model.Fault;
import com.chargedot.charge.service.FaultService;
import com.chargedot.charge.util.GeneratorUtil;
import com.chargedot.charge.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/30
 */
@Slf4j
public class SaveErrorInfo implements Runnable {

    private FaultService faultService;
    private StringRedisTemplate stringRedisTemplate;
    private String key;
    private String message;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SaveErrorInfo(String key, String message, FaultService faultService, StringRedisTemplate stringRedisTemplate) {
        this.key = key;
        this.message = message;
        this.faultService = faultService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void run() {
        Map mapParam = JacksonUtil.json2Bean(message, Map.class);
        String operationType = (String) mapParam.get("OperationType");
        String port = (String) mapParam.get("Port");
        if(StringUtils.isNotBlank(port)){
            key = key + "-" + port;
        }
        switch (operationType){
            case ConstantConfig.DReportDeviceFaultRequest:
                log.info("[kafkaMessage][deviceNumber]{}[message]{}", key, message);
                Integer deviceId = (Integer) mapParam.get("DeviceId"); //设备ID
                Integer faultType = (Integer) mapParam.get("FaultType"); //错误类型 255：保留，254：设备故障（非充电模块故障），253：设备告警，0-223：故障模块地址（显示时显示对应的二进制字符串）
                Integer stationId = (Integer) mapParam.get("StationId"); //设备所属站点ID
                Integer faultFlag = (Integer) mapParam.get("FaultFlag"); //0：错误发生 1：错误恢复
                Integer deviceType = (Integer) mapParam.get("DeviceType");//1：交流，2：直流，3：交直流一体机，4：充电堆
                Integer faultCode = (Integer) mapParam.get("FaultCode"); //错误代码
                Integer timestamp = (Integer) mapParam.get("Timestamp");//时间戳


                Fault fault = new Fault();
                fault.setDeviceId(deviceId);
                fault.setDeviceType(deviceType);
                fault.setStationId(stationId);
                fault.setFaultType(faultType);
                fault.setFaultCode(faultCode);
                if(!port.equals("FF") && !port.equals("00") ){
                    fault.setPort(port);
                    fault.setPortId((Integer) mapParam.get("PortId"));
                }else{
                    fault.setPort("");
                    fault.setPortId(0);
                }

                if(faultFlag == 0){
                    String faultInfo = (String) stringRedisTemplate.opsForHash().get(ConstantConfig.REDIS_DEVICE_FAULT, key + faultType);
                    if(StringUtils.isBlank(faultInfo)){
                        fault.setId(GeneratorUtil.getInstance().generateId(String.valueOf(deviceId)));
                        fault.setStartedAt(sdf.format(timestamp * 1000L));
                        stringRedisTemplate.opsForHash().put(ConstantConfig.REDIS_DEVICE_FAULT, key + faultType, JacksonUtil.bean2Json(fault));
                        try {
                            faultService.insertFaultRecord(fault);
                        } catch (InsertException e) {
                            stringRedisTemplate.opsForHash().delete(ConstantConfig.REDIS_DEVICE_FAULT, key + faultType);
                            log.info("exception happened {}", e);
                        }
                    }else {
                        log.info("redis has the same type of error record, no need to add record!!!");
                    }
                }else if(faultFlag == 1){
                    String faultInfo = (String) stringRedisTemplate.opsForHash().get(ConstantConfig.REDIS_DEVICE_FAULT, key + faultType);
                    if(StringUtils.isNotBlank(faultInfo)){
                        Fault faultFromRedis = JacksonUtil.json2Bean(faultInfo, Fault.class);
                        String id = faultFromRedis.getId();
                        try{
                            faultService.updateFaultRecord(id, sdf.format(timestamp * 1000L));
                            stringRedisTemplate.opsForHash().delete(ConstantConfig.REDIS_DEVICE_FAULT, key + faultType);
                        }catch (UpdateException e){
                            log.info("exception happened, not need clear redis cache!!!");
                            log.info("exception happened {}", e);
                        }
                    }else {
                        log.info("redis has not start record, no need to update record!!!");
                    }
                } else {
                    log.info("faultFlag not exist...");
                }
                break;
        }
    }
}

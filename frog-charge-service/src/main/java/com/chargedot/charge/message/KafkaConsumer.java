package com.chargedot.charge.message;

import com.chargedot.charge.config.ConstantConfig;
import com.chargedot.charge.service.FaultService;
import com.chargedot.charge.util.JsonValidatorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/30
 */
@Component
@Slf4j
public class KafkaConsumer {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private FaultService faultService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService faultHandleThreadPool = Executors.newFixedThreadPool(1);

    private ExecutorService chargeControlThreadPool = Executors.newFixedThreadPool(3);

    @KafkaListener(topics = {ConstantConfig.DWD_ERROR_REQ,
            ConstantConfig.DWD_START_STOP_RESULT_REQ,
            ConstantConfig.DWD_CHECK_IN_REQ,
            ConstantConfig.D2S_REQ_TOPIC})
    public void consumer(ConsumerRecord<String, String> consumer) {
        String topic = "";
        String key = "";
        String message = "";
        if (consumer.topic() != null) {
            topic = consumer.topic();
        }
        if (consumer.key() != null) {
            key = consumer.key();
        }
        if (consumer.value() != null) {
            message = consumer.value();
        }

        if (topic == null || "".equals(topic.trim()) || " ".equals(topic)) {
            log.info("[invalid topic]{}", topic);
            return;
        }
        if (key == null || "".equals(key.trim()) || " ".equals(key)) {
            log.info("[invalid key]{}", key);
            return;
        }
        if (!new JsonValidatorUtil().validate(message)) {
            log.info("[invalid message string]{}", message);
            return;
        }

        if (ConstantConfig.DWD_ERROR_REQ.equals(topic)) {
            faultHandleThreadPool.execute(new SaveErrorInfo(key, message, faultService, stringRedisTemplate));
        } else if (ConstantConfig.DWD_START_STOP_RESULT_REQ.equals(topic)
                || ConstantConfig.DWD_CHECK_IN_REQ.equals(topic)
                || ConstantConfig.D2S_REQ_TOPIC.equals(topic)) {
            chargeControlThreadPool.execute(new MessageHandler(key, message));
        }
    }

}

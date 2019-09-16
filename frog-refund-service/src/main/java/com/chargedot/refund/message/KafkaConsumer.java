package com.chargedot.refund.message;

import com.chargedot.refund.config.ConstantConfig;
import com.chargedot.refund.util.JsonValidatorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

    private ExecutorService chargeRefundThreadPool = Executors.newFixedThreadPool(3);

    @KafkaListener(topics = {ConstantConfig.DW_CHARGE_REFUND_TOPIC})
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

        if (ConstantConfig.DW_CHARGE_REFUND_TOPIC.equals(topic)) {
            chargeRefundThreadPool.execute(new MessageHandler(key, message));
        }
    }

}

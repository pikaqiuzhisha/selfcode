package com.chargedot.charge.message;

import com.chargedot.charge.util.JacksonUtil;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.Future;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2017/12/27
 */
@Component
public class KafkaProducer {

    private Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * singleton instance
     */
    private static KafkaProducer instance = null;

    /**
     * get instance
     *
     * @return instance
     */
    public static KafkaProducer getInstance() {
        if (instance == null) {
            instance = new KafkaProducer();
        }
        return instance;
    }

    public Future<RecordMetadata> send(String topic, String key, String message) {

        ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, message);
        Future<RecordMetadata> result = kafkaTemplate.send(topic, key, message);
        return result;
    }

    //    @Scheduled(fixedDelay = 10000)
    public void sendTest() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("OperationType", "PUpgradeRequest");
        params.put("Provider", 2);
        params.put("Version", "V1.00.02");
        params.put("PacketLen", 64644);
        params.put("Checksum", "87C5CD84");
        params.put("UpBytes", 512);
        params.put("UpMode", 1);
        params.put("DownPath", "dev.chargedot.com;chargedot;test.cdot1202;8722;/mnt/workspace/online/cdot-admin-sandbox/public/tcproot/102/5401860090/10010057102;f1_ucosii.bin");
        params.put("StorePath", "LHZGEEK20180421133727357/LHZGEEK.zip");
        LOG.info("[send message]{}", JacksonUtil.bean2Json(params));
        kafkaTemplate.send("DWS-UPGRADE-REQ", "test12345", JacksonUtil.bean2Json(params));

    }

}

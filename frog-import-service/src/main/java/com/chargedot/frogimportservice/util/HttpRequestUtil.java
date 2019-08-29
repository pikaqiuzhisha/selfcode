package com.chargedot.frogimportservice.util;

import com.chargedot.frogimportservice.config.ConstantConfig;
import com.chargedot.frogimportservice.config.CustomConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/5/31
 */
@Slf4j
public class HttpRequestUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestUtil.class);


    public static String sendRefundRequest(String deviceNumber, String port, int userId, int timeStamp, CustomConfig config, int refundFlag){

        String result = null;
        try{
            RestTemplate restTemplate = new RestTemplate(httpClientFactory());
            String refund = "elec_frog:cdot_elec_frog:" + System.currentTimeMillis();
            byte[] encodeRefund = Base64.encodeBase64(refund.getBytes(Charset.forName("UTF-8")));
            String refundverify = new String(encodeRefund);

            String auth = "elec_frog:081b8a0a6d179d56feccb0d7b1b2d013";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
            String authHeader = "Basic " + new String(encodedAuth);
            HttpHeaders httpHeaders = new HttpHeaders();
//            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            httpHeaders.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_UTF8_VALUE));
            httpHeaders.add("Authorization", authHeader);
            httpHeaders.add("refundverify", refundverify);
            httpHeaders.add("r", String.valueOf(System.currentTimeMillis() / 1000));
//            MultiValueMap<String, String> params= new LinkedMultiValueMap<>();
//            params.add("port", port);
//            params.add("deviceNumber", deviceNumber);
//            params.add("userId", String.valueOf(userId));
//            params.add("timeStamp", String.valueOf(timeStamp));

            Map<String, String> params = new HashMap<>();
            params.put("port", port);
            params.put("deviceNumber", deviceNumber);
            params.put("userId", String.valueOf(userId));
            params.put("timeStamp",  String.valueOf(timeStamp));
            params.put("refundFlag", String.valueOf(refundFlag));
            log.info("[refundToBalance][params]{}", JacksonUtil.bean2Json(params));
            HttpEntity<String> entity = new HttpEntity(params, httpHeaders);
            String url = config.getUrl() + ConstantConfig.REFUND_TO_BALANCE;
            log.info("[refundToBalance][url]{}", url);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            result = responseEntity.getBody();

        }catch (Exception e){
            log.info("exception happened ", e);
        }
        return result;
    }

    public static String notifyUserRequest(String deviceNumber, String port, int userId, int timeStamp, String reason, String interfaceName, CustomConfig config){
        String result = null;

        try {
            RestTemplate restTemplate = new RestTemplate(httpClientFactory());

            String auth = "elec_frog:081b8a0a6d179d56feccb0d7b1b2d013";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
            String authHeader = "Basic " + new String(encodedAuth);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_UTF8_VALUE));
            httpHeaders.add("Authorization", authHeader);
            httpHeaders.add("r", String.valueOf(System.currentTimeMillis() / 1000));

            Map<String, String> params = new HashMap<>();
            params.put("port", port);
            params.put("deviceNumber", deviceNumber);
            params.put("userId", String.valueOf(userId));
            params.put("timeStamp", String.valueOf(timeStamp));
            params.put("reason", reason);
            log.info("[finishCharge][params]{}", JacksonUtil.bean2Json(params));
            HttpEntity<String> entity = new HttpEntity(params, httpHeaders);
            String url = config.getUrl() + interfaceName;
            log.info("[finishCharge][url]{}", url);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            result = responseEntity.getBody();
        }catch (Exception e){
            log.info("exception happened ", e);
        }

        return result;
    }

    public static String reconnectRefund(String deviceNumber, CustomConfig config, int connectNetMode) {
        String result = null;

        try {
            RestTemplate restTemplate = new RestTemplate(httpClientFactory());

            String auth = "elec_frog:081b8a0a6d179d56feccb0d7b1b2d013";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
            String authHeader = "Basic " + new String(encodedAuth);

            String refund = "elec_frog:cdot_elec_frog:" + System.currentTimeMillis();
            byte[] encodeRefund = Base64.encodeBase64(refund.getBytes(Charset.forName("UTF-8")));
            String refundverify = new String(encodeRefund);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_UTF8_VALUE));
            httpHeaders.add("Authorization", authHeader);
            httpHeaders.add("r", String.valueOf(System.currentTimeMillis() / 1000));
            httpHeaders.add("refundverify", refundverify);

            Map<String, String> params = new HashMap<>();
            params.put("deviceNumber", deviceNumber);
            params.put("reason", String.valueOf(connectNetMode));
            log.info("[reconnectRefund][params]{}", JacksonUtil.bean2Json(params));
            HttpEntity<String> entity = new HttpEntity(params, httpHeaders);
            String url = config.getUrl() + ConstantConfig.RECONNECT_REFUND;
            log.info("[reconnectRefund][url]{}", url);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            result = responseEntity.getBody();
        }catch (Exception e){
            log.info("exception happened ", e);
        }
        return result;
    }

    /**
     * restTemplate post request set timeout
     * @return
     */
    public static HttpComponentsClientHttpRequestFactory httpClientFactory(){

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setReadTimeout(35000);
        httpRequestFactory.setConnectTimeout(35000);
        httpRequestFactory.setConnectionRequestTimeout(35000);

        return httpRequestFactory;
    }

}

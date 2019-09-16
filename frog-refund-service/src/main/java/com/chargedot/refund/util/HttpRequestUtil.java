package com.chargedot.refund.util;

import com.chargedot.refund.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
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

    public static String notifyUserRequest(String orderNumber, String reason, String interfaceName, String userPushUrl){
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
            params.put("orderNumber", orderNumber);
            params.put("reason", reason);
            HttpEntity<String> entity = new HttpEntity(params, httpHeaders);
            String url = userPushUrl + interfaceName;
            log.info("[finishCharge][url][params]{} {}", url, JacksonUtil.bean2Json(params));
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

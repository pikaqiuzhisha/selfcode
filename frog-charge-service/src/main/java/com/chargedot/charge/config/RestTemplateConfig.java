package com.chargedot.charge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/3/15
 */

@Component
public class RestTemplateConfig {
    private Logger LOG = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Value("${rest.readTimeout}")
    private int readTimeout;
    @Value("${rest.connectTimeout}")
    private int connectionTimeout;
    @Value("${rest.connectionRequestTimeout}")
    private int connectionRequestTimeout;


    @Bean
    public HttpComponentsClientHttpRequestFactory httpClientFactory() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setReadTimeout(readTimeout);
        httpRequestFactory.setConnectTimeout(connectionTimeout);
        httpRequestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
        return httpRequestFactory;
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(HttpComponentsClientHttpRequestFactory httpClientFactory) {
        RestTemplate restTemplate = new RestTemplate(httpClientFactory);
        return restTemplate;
    }

}

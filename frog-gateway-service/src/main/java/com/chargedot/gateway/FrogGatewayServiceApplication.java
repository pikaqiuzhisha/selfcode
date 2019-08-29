package com.chargedot.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Eric Gui
 * @date 2019/8/28
 */
@SpringBootApplication
@EnableEurekaClient
public class FrogGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrogGatewayServiceApplication.class, args);
    }
}

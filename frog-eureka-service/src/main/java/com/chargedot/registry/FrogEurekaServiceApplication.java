package com.chargedot.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @author Eric Gui
 * @date 2019/8/21
 */
@SpringBootApplication
@EnableEurekaServer
public class FrogEurekaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrogEurekaServiceApplication.class, args);
    }
}

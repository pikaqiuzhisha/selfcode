package com.chargedot.refund;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Eric Gui
 * @date 2019/9/5
 */
@SpringBootApplication
@EnableEurekaClient
@EnableTransactionManagement
public class FrogRefundServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrogRefundServiceApplication.class, args);
    }
}

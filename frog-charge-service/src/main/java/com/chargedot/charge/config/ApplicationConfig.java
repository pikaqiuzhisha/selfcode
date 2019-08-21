package com.chargedot.charge.config;

import com.chargedot.charge.handler.RequestHandler;
import com.chargedot.charge.util.SpringBeanUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @author Eric Gui
 * @date 2018/12/20
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public SpringBeanUtil springBeanUtil() {
        return new SpringBeanUtil();
    }

    @Bean
    @DependsOn("springBeanUtil")
    public RequestHandler requestHandler() {
        return new RequestHandler();
    }
}

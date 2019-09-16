package com.chargedot.refund.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Eric Gui
 * @date 2018/12/20
 */
@Configuration
@Data
public class ServerConfig {

    @Value("${request.message.queue.capacity}")
    public int requestMessageQueueCapacity;

    @Value("${request.message.handler.count}")
    public int requestMessageHandlerCount;

    @Value("${charge.rate.default.hour}")
    public int defaultChargeRateHour;

    @Value("${charge.rate.default.fee}")
    public int defaultChargeRateFee;

    @Value("${charge.rate.default.detail}")
    public String defaultFeeDetailSnap;

    @Value("${push.user.url}")
    public String userPushUrl;
}

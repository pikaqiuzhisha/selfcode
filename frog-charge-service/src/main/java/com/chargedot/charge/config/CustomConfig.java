package com.chargedot.charge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/2/28
 */
@ConfigurationProperties(prefix = "config")
@Component
public class CustomConfig {

    private String url;

    private String presetChargeTime;

    private String actualChargeTime;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPresetChargeTime() {
        return presetChargeTime;
    }

    public void setPresetChargeTime(String presetChargeTime) {
        this.presetChargeTime = presetChargeTime;
    }

    public String getActualChargeTime() {
        return actualChargeTime;
    }

    public void setActualChargeTime(String actualChargeTime) {
        this.actualChargeTime = actualChargeTime;
    }
}

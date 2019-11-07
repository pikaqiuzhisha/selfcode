package com.chargedot.wechat.config;

import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PayConfig {

    @Autowired
    private WechatAccountConfig accountConfig;

    @Autowired
    private AliPayAccountConfig aliPayAccountConfig;

    @Bean
    public WxPayConfig wxPayConfig() {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(accountConfig.getMpAppId());
        wxPayConfig.setMiniAppId(accountConfig.getMiniAppId());
        wxPayConfig.setMchId(accountConfig.getMchId());
        wxPayConfig.setMchKey(accountConfig.getMchKey());
        wxPayConfig.setKeyPath(accountConfig.getKeyPath());
        wxPayConfig.setNotifyUrl(accountConfig.getNotifyUrl());
        wxPayConfig.setAppAppId(accountConfig.getAppAppId());
        return wxPayConfig;
    }

    @Bean
    public BestPayServiceImpl bestPayService(WxPayConfig wxPayConfig) {
        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
        bestPayService.setWxPayConfig(wxPayConfig);
        return bestPayService;
    }
}

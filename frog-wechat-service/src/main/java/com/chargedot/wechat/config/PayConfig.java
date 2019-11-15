package com.chargedot.wechat.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayConfig extends WxPayConfig {
    @Autowired
    private WechatAccountConfig accountConfig;
    @Autowired
    private WxPayConfig wxPayConfig;

    /**
     * 微信支付是否使用仿真测试环境.
     * 默认不使用
     */
//    private boolean useSandboxEnv = true;
    @Bean
    public WxPayConfig wxPayConfig() {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(accountConfig.getMpAppId());
        wxPayConfig.setMchId(accountConfig.getMchId());
        wxPayConfig.setMchKey(accountConfig.getMchKey());
        wxPayConfig.setKeyPath(accountConfig.getKeyPath());
        wxPayConfig.setNotifyUrl(accountConfig.getNotifyUrl());
        return wxPayConfig;
    }

    @Bean
    public WxPayServiceImpl wxPayService(WxPayConfig wxPayConfig) {
        WxPayServiceImpl wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(wxPayConfig);
        return wxPayService;
    }
}

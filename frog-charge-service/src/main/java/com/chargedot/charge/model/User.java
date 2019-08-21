package com.chargedot.charge.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Data
public class User {

    /**
     * 用户ID
     */
    private Integer id;

    /**
     * 归属公司ID
     */
    private Integer enterpriseId;

    /**
     * 用户类型：默认1纯手机用户,2为绑定过app的用户3包月包时卡用户4包月包次5充值卡用户
     */
    private Integer type;

    /**
     * 账户余额
     */
    private Integer balance;

    /**
     * 封禁状态：默认1正常2封禁
     */
    private Integer forbidStatus;

    /**
     * 注册状态：默认1未注册2已注册
     */
    private Integer status;

    /**
     * 注册渠道:默认1微信公众号注册2app端注册3平台注册4小程序注册
     */
    private Integer userSrc;

    /**
     * 卡用户标识：默认1非月卡用户2月电子卡用户3物理卡
     */
    private Integer monthCard;
}

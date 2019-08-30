package com.chargedot.frogimportservice.service;

import com.chargedot.frogimportservice.model.ChargeCard;

import java.util.List;

public interface ChargeCardService {

    /**
     * 批量导入卡号信息
     * @param chargeCardList 卡号集合
     * @return 返回受影响行数
     */
    int importChargeCardData(List<ChargeCard> chargeCardList);

    /**
     * 新增卡号信息
     * @param chargeCard 卡号
     * @return 返回受影响行数
     */
    int addChargeCardInfo(ChargeCard chargeCard);

    /**
     * 查询重复的卡号
     * @param cardNumber 卡号
     * @return 返回 “true” 或 “false”
     */
    boolean selectChargeCardNumberCount(String cardNumber);
}

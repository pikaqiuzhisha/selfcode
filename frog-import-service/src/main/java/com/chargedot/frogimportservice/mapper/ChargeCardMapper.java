package com.chargedot.frogimportservice.mapper;

import com.chargedot.frogimportservice.model.ChargeCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChargeCardMapper {

    /**
     * 批量导入卡号信息
     * @param chargeCardList 卡号集合
     * @return 返回受影响行数
     */
    int importChargeCardData(@Param("chargeCardList")List<ChargeCard> chargeCardList);

    /**
     * 查询重复的卡号
     * @param cardNumber 卡号
     * @return 返回个数
     */
    int selectChargeCardNumberCount(@Param("cardNumber")String cardNumber);
}

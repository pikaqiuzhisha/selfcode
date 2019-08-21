package com.chargedot.charge.mapper;

import com.chargedot.charge.model.ChargeCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Mapper
public interface ChargeCardMapper {

    /**
     * 根据卡号查找卡片信息
     * @param cardNumber
     * @return
     */
    ChargeCard findByCardNumber(@Param("cardNumber") String cardNumber);

    /**
     * 根据卡用户ID查找卡片信息
     */
    ChargeCard findByUserId(@Param("userId") Integer userId);

    /**
     * 更新卡信息
     * @param chargeCard
     */
    void updateCardStatus(ChargeCard chargeCard);


}

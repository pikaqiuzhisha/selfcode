package com.chargedot.charge.model;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/26
 */
@Data
public class CardStream {

    private Long id;

    private Integer orderId;

    private Long cardId;

    private Integer userId;

    private String beginedAt;

    private String finishedAt;

    /**
     * 1为扣费2为充值3为退款
     */
    private Integer streamType;

    private Integer preValue;

    private Integer changeValue;

    private Integer curValue;

    private Integer realPreValue;

    private Integer realChangeValue;

    private Integer curRealValue;

    /**
     * 1来自平台2来自小程序3刷卡
     */
    private Integer operatorSrc;

    private Integer operatorId;

    public void setter(int orderId, long cardId, int userId, String beginedAt, String finishedAt, int streamType, int preValue, int changeValue, int curValue,
                       int realPreValue, int realChangeValue, int curRealValue, int operatorSrc, int operatorId) {
        this.setOrderId(orderId);
        this.setCardId(cardId);
        this.setUserId(userId);
        this.setBeginedAt(beginedAt);
        this.setFinishedAt(finishedAt);
        this.setStreamType(streamType);
        this.setPreValue(preValue);
        this.setChangeValue(changeValue);
        this.setCurValue(curValue);
        this.setRealPreValue(realPreValue);
        this.setRealChangeValue(realChangeValue);
        this.setCurRealValue(curRealValue);
        this.setOperatorSrc(operatorSrc);
        this.setOperatorId(operatorId);
    }
}

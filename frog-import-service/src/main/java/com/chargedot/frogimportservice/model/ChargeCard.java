package com.chargedot.frogimportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Data
public class ChargeCard {

    /**
     * ID
     */
    private Long id;

    /**
     * 卡号
     */
    @JsonProperty("CardNumber")
    private String cardNumber;

    /**
     * 卡用户ID
     */
    private Integer userId;

    /**
     * 卡公司
     */
    @JsonProperty("EnterpriseId")
    private Integer enterpriseId;

    /**
     * 卡类型（卡类型默认1包月次卡 2包月包时卡 3充值卡）
     */
    @JsonProperty("Type")
    private Integer type;

    /**
     * 卡有效期开始时间
     */
    private String beginedAt;

    /**
     * 卡有效期结束时间
     */
    @JsonProperty("FinishedAt")
    private String finishedAt;

    /**
     * 最大使用次数或者最大可用时长
     */
    private Integer maxValue;

    /**
     * 当前剩余有效次数或者有效时长
     */
    private Integer curValue;

    /**
     * 当存储金额该字段表示实体账户
     */
    private  Integer realValue;

    /**
     * 卡片封禁状态（默认1已激活2封禁）
     */
    private Integer forbidStatus;

    /**
     * 卡片使用状态（默认1可使用2使用中3不可使用）
     */
    private Integer cardStatus;

    /**
     * 卡备注信息
     */
    private String remarks;

    /**
     * 是否删除
     */
    private Integer isDel;

    /**
     * 创建日期
     */
    private String createdAt;

    /**
     * 修改日期
     */
    private String  updatedAt;

    /**
     * 删除日期
     */
    private String deletedAt;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getBeginedAt() {
        return beginedAt;
    }

    public void setBeginedAt(String beginedAt) {
        this.beginedAt = beginedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public Integer getCurValue() {
        return curValue;
    }

    public void setCurValue(Integer curValue) {
        this.curValue = curValue;
    }

    public Integer getRealValue() {
        return realValue;
    }

    public void setRealValue(Integer realValue) {
        this.realValue = realValue;
    }

    public Integer getForbidStatus() {
        return forbidStatus;
    }

    public void setForbidStatus(Integer forbidStatus) {
        this.forbidStatus = forbidStatus;
    }

    public Integer getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(Integer cardStatus) {
        this.cardStatus = cardStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getIsDel() {
        return isDel;
    }

    public void setIsDel(Integer isDel) {
        this.isDel = isDel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }
}

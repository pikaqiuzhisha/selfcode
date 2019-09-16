package com.chargedot.charge.model;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
public class ChargeCert {

    /**
     * ID
     */
    private Long id;

    /**
     * 充电凭证
     */
    private String certNumber;

    /**
     * 所属用户ID
     */
    private int enterpriseId;

    /**
     * 关联用户ID
     */
    private Integer userId;

    /**
     * 凭证类型（1包月次卡 2包月包时卡 3充值卡 4手机号）
     */
    private Integer type;

    /**
     * 有效期开始时间
     */
    private String beginedAt;

    /**
     * 有效期结束时间
     */
    private String finishedAt;

    /**
     * 最大使用次数或者最大可用时长
     */
    private Integer maxValue;

    /**
     * 当前剩余有效次数或者有效时长，或者虚拟账户余额
     */
    private Integer curValue;

    /**
     * 实账余额
     */
    private Integer realValue;

    /**
     * 卡片封禁状态（默认1已激活2封禁）
     */
    private Integer forbidStatus;

    /**
     * 使用状态（1可使用 2使用中 3不可使用）
     */
    private Integer certStatus;

    private Integer cardStatus;

    public ChargeCert() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCertNumber() {
        return certNumber;
    }

    public void setCertNumber(String certNumber) {
        this.certNumber = certNumber;
    }

    public int getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(int enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public Integer getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(Integer certStatus) {
        this.certStatus = certStatus;
    }

    public Integer getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(Integer cardStatus) {
        this.cardStatus = cardStatus;
    }
}

package com.chargedot.frogimportservice.model;

import lombok.Data;

@Data
public class DWCert {

    /**
     * ID
     */
    private Long id;

    /**
     * 凭证号(手机号、实体卡或电子卡号)
     */
    private String certNumber;

    /**
     * 关联的user表中用户ID
     */
    private Integer userId;

    /**
     * 该凭证归属公司
     */
    private Integer enterpriseId;

    /**
     * 凭证类型默认1包月次卡 2包月包时卡 3充值卡 4手机号
     */
    private Integer type;

    /**
     * 起始有效日期
     */
    private String beginedAt;

    /**
     * 截止有效日期
     */
    private String finishedAt;

    /**
     * 最大额度用于批量每天更新,当次卡存储次数，时卡存储时长单位分钟
     */
    private Integer maxValue;

    /**
     * 当前剩余额度当次卡存储次数，时卡存储时长单位分钟,当存储金额该字段表示虚拟账户单位分
     */
    private Integer curValue;

    /**
     * 表示实体账户单位分
     */
    private Integer realValue;

    /**
     * 默认1已激活2封禁
     */
    private Integer forbidStatus;

    /**
     * 默认1可使用2使用中3不可使用
     */
    private Integer certStatus;

    /**
     * 凭证备注信息
     */
    private String remarks;

    /**
     * 是否删除
     */
    private Integer isDel;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 修改时间
     */
    private String updatedAt;

    /**
     * 删除时间
     */
    private String deletedAt;

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

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }
}

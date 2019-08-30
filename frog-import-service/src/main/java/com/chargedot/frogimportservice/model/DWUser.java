package com.chargedot.frogimportservice.model;

import lombok.Data;

@Data
public class DWUser {
    /**
     * ID
     */
    private Integer id;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String name;

    /**
     * 电子邮件
     */
    private String email;

    /**
     * 真实名字
     */
    private String realName;

    /**
     *
     */
    private String face;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private String birthday;

    /**
     * 国家ID
     */
    private Integer countryId;

    /**
     * 省份ID
     */
    private Integer provinceId;

    /**
     * 城市ID
     */
    private Integer cityId;

    /**
     * 街区ID
     */
    private Integer districtId;

    /**
     * 地址
     */
    private String address;

    /**
     * 类型（默认1.纯手机用户 2.为绑定过app用户 3.包月包时卡用户 4.包月包次卡用户 5.充值卡用户）
     */
    private Integer type;

    /**
     * 表示实体账户
     */
    private Integer balance;

    /**
     * 表示虚拟账户
     */
    private Integer virtualBalance;

    /**
     *
     */
    private String drawScoreAt;

    /**
     * 封禁状态（默认1.正常 2.封禁）
     */
    private Integer forbidStatus;

    /**
     * 注册状态（默认1.未注册 2.注册）
     */
    private Integer status;

    /**
     * 注册渠道（默认1.微信公众号注册 2.app端注册 3。平台注册）
     */
    private Integer userSrc;

    /**
     * 月卡（默认1.非月卡用户 2.月卡用户 3.物理月卡）
     */
    private Integer monthCard;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 归属公司
     */
    private Integer enterpriseId;

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
    private String updatedAt;

    /**
     * 删除日期
     */
    private String deletedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }

    public Integer getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Integer provinceId) {
        this.provinceId = provinceId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Integer getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Integer districtId) {
        this.districtId = districtId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Integer getVirtualBalance() {
        return virtualBalance;
    }

    public void setVirtualBalance(Integer virtualBalance) {
        this.virtualBalance = virtualBalance;
    }

    public String getDrawScoreAt() {
        return drawScoreAt;
    }

    public void setDrawScoreAt(String drawScoreAt) {
        this.drawScoreAt = drawScoreAt;
    }

    public Integer getForbidStatus() {
        return forbidStatus;
    }

    public void setForbidStatus(Integer forbidStatus) {
        this.forbidStatus = forbidStatus;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getUserSrc() {
        return userSrc;
    }

    public void setUserSrc(Integer userSrc) {
        this.userSrc = userSrc;
    }

    public Integer getMonthCard() {
        return monthCard;
    }

    public void setMonthCard(Integer monthCard) {
        this.monthCard = monthCard;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
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

package com.chargedot.frogimportservice.model.vo;

import com.chargedot.frogimportservice.model.DWCert;
import lombok.Data;

import java.util.List;

@Data
public class DataParam {
    /**
     * 卡号集合
     */
    private List<DWCert> dwCertList;

    /**
     * 卡类型
     */
    private Integer type;

    /**
     * 归属公司
     */
    private Integer enterpriseId;

    /**
     * 有效日期
     */
    private  String finishedAt;

    public List<DWCert> getDwCertList() {
        return dwCertList;
    }

    public void setDwCertList(List<DWCert> dwCertList) {
        this.dwCertList = dwCertList;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }
}

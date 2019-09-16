package com.chargedot.frogimportservice.service;

import com.chargedot.frogimportservice.model.DWCert;

import java.util.List;

public interface DWCertService {

    /**
     * 批量导入凭证号信息
     * @param dwCertList 凭证号集合
     * @return 返回受影响行数
     */
    int importDWCertData(List<DWCert> dwCertList);

    /**
     * 查询重复的凭证号
     * @param certNumber 凭证号
     * @return 返回 “true” 或 “false”
     */
    boolean selectDWCertNumberCount(String certNumber);
}

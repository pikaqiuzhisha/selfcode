package com.chargedot.frogimportservice.mapper;

import com.chargedot.frogimportservice.model.DWCert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DWCertMapper {

    /**
     * 批量导入凭证号信息
     * @param dwCertList 凭证号集合
     * @return 返回受影响行数
     */
    int importDWCertData(@Param("dwCertList") List<DWCert> dwCertList);

    /**
     * 查询重复的凭证号
     * @param certNumber 凭证号
     * @return 返回个数
     */
    int selectDWCertNumberCount(@Param("certNumber")String certNumber);
}

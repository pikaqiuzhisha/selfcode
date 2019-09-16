package com.chargedot.refund.mapper;

import com.chargedot.refund.model.ChargeCert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Mapper
public interface ChargeCertMapper {

    /**
     * 根据充电凭证号查找凭证信息
     * @param certNumber
     * @return
     */
    ChargeCert findByCertNumber(@Param("certNumber") String certNumber);

    /**
     * 根据凭证ID查找凭证信息
     */
    ChargeCert findByCertId(@Param("certId") long certId);

    /**
     * 更新凭证信息
     * @param chargeCert
     */
    void updateCertStatus(ChargeCert chargeCert);


}

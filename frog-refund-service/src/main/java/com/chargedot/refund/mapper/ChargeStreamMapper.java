package com.chargedot.refund.mapper;

import com.chargedot.refund.model.ChargeStream;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Eric Gui
 * @date 2019/4/26
 */
@Mapper
public interface ChargeStreamMapper {

    void insert(ChargeStream chargeStream);
}

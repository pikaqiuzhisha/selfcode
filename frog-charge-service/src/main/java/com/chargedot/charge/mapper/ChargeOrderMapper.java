package com.chargedot.charge.mapper;

import com.chargedot.charge.model.ChargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Mapper
public interface ChargeOrderMapper {

    ChargeOrder findBySequenceNumber(@Param("sequenceNumber") String sequenceNumber);

    ChargeOrder findByPortIdLast(@Param("portId") Integer portId);

    void insert(ChargeOrder chargeOrder);

    void update(ChargeOrder chargeOrder);

    void updatePayStream(@Param("sequenceNumber") String sequenceNumber, @Param("payStreamId") Long payStreamId);

    void refundUpdate(ChargeOrder chargeOrder);

    void updateRefundStream(@Param("sequenceNumber") String sequenceNumber, @Param("refundStreamId") Long refundStreamId);


}

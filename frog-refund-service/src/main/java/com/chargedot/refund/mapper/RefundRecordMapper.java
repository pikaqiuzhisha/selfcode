package com.chargedot.refund.mapper;

import com.chargedot.refund.model.RefundRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundRecordMapper {

    /**
     * 插入一条退款记录
     *
     * @param refundRecord 退款记录对象
     */
    void insertRefundRecord(RefundRecord refundRecord);
}

package com.chargedot.wechat.mapper;

import com.chargedot.wechat.model.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RefundRecordMapper {

    /**
     * 得到退款记录
     *
     * @return 返回退款记录信息
     */
    List<RefundRecord> getRefundRecord();

    /**
     * 插入一条退款记录
     *
     * @param refundRecord 退款记录对象
     */
    void insertRefundRecord(RefundRecord refundRecord);

    /**
     * 更新退款记录的状态
     *
     * @param refundRecord 退款记录对象
     */
    void updateRefundRecord(RefundRecord refundRecord);


    /**
     * 根据退款单号得到退款单信息
     *
     * @param refundNumber 退款单号
     * @return 返回退款单号信息
     */
    RefundRecord getRefundRecordByRefundNumber(@Param("refundNumber") String refundNumber);
}

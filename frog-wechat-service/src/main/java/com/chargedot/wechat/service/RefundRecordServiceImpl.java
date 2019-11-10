package com.chargedot.wechat.service;

import com.chargedot.wechat.mapper.RefundRecordMapper;
import com.chargedot.wechat.model.RefundRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RefundRecordServiceImpl implements RefundRecordService {

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Override
    public List<RefundRecord> getRefundRecord() {
        return refundRecordMapper.getRefundRecord();
    }

    @Override
    @Transactional
    public void insertRefundRecord(RefundRecord refundRecord) {
        refundRecordMapper.insertRefundRecord(refundRecord);
    }

    @Override
    @Transactional
    public void updateRefundRecord(RefundRecord refundRecord) {
        refundRecordMapper.updateRefundRecord(refundRecord);
    }

    @Override
    public RefundRecord getRefundRecordByOrderId(String outTradeNo) {
        return refundRecordMapper.getRefundRecordByOrderId(outTradeNo);
    }
}

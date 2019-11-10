package com.chargedot.wechat.service;

import com.chargedot.wechat.mapper.ChargeOrderMapper;
import com.chargedot.wechat.model.ChargeOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargeOrderServiceImpl implements ChargeOrderService {

    @Autowired
    private ChargeOrderMapper chargeOrderMapper;

    @Override
    public ChargeOrder findByOrderNumber(String orderNumber) {
        return chargeOrderMapper.findByOrderNumber(orderNumber);
    }

    @Override
    @Transactional
    public void updateChargeOrder(ChargeOrder chargeOrder) {
        chargeOrderMapper.updateChargeOrder(chargeOrder);
    }
}

package com.chargedot.frogimportservice.service;

import com.chargedot.frogimportservice.mapper.ChargeCardMapper;
import com.chargedot.frogimportservice.model.ChargeCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ChargeCardServiceImpl implements ChargeCardService{

    @Autowired
    private ChargeCardMapper chargeCardMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)
    public int importChargeCardData(List<ChargeCard> chargeCardList) {
        return chargeCardMapper.importChargeCardData(chargeCardList);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)
    public int addChargeCardInfo(ChargeCard chargeCard) {
        return chargeCardMapper.addChargeCardInfo(chargeCard);
    }

    @Override
    public boolean selectChargeCardNumberCount(String cardNumber) {
        return chargeCardMapper.selectChargeCardNumberCount(cardNumber) > 0;
    }
}

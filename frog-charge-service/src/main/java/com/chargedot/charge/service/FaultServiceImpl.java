package com.chargedot.charge.service;

import com.chargedot.charge.exception.InsertException;
import com.chargedot.charge.exception.UpdateException;
import com.chargedot.charge.mapper.FaultMapper;
import com.chargedot.charge.model.Fault;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/1/18
 */
@Service
@Slf4j
public class FaultServiceImpl implements FaultService {

    @Autowired
    private FaultMapper faultMapper;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)
    public void insertFaultRecord(Fault fault) throws InsertException {
        try{
            faultMapper.insert(fault);
        }catch (Exception e){
            throw new InsertException();
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)
    public void updateFaultRecord(String id, String finishedAt) throws UpdateException {
        try{
            faultMapper.update(id, finishedAt);
        }catch (Exception e){
            throw new UpdateException();
        }
    }

}

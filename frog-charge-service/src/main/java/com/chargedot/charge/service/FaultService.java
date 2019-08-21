package com.chargedot.charge.service;

import com.chargedot.charge.exception.InsertException;
import com.chargedot.charge.exception.UpdateException;
import com.chargedot.charge.model.Fault;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/4/24
 */
public interface FaultService {

    void insertFaultRecord(Fault fault) throws InsertException;

    void updateFaultRecord(String id, String finishedAt) throws UpdateException;

}

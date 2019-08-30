package com.chargedot.frogimportservice.service;

import com.chargedot.frogimportservice.mapper.DWUserMapper;
import com.chargedot.frogimportservice.model.DWUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DWUserServiceImpl implements DWUserService {

    @Autowired
    private DWUserMapper dwUserMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)
    public int importDWUserData(List<DWUser> dwUserList) {
        return dwUserMapper.importDWUserData(dwUserList);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)
    public int addDWUserInfo(DWUser dwUser) {
        return dwUserMapper.addDWUserInfo(dwUser);
    }

}

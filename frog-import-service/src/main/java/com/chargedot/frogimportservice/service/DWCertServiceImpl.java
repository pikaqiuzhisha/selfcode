package com.chargedot.frogimportservice.service;

import com.chargedot.frogimportservice.mapper.DWCertMapper;
import com.chargedot.frogimportservice.model.DWCert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DWCertServiceImpl implements DWCertService {

    @Autowired
    private DWCertMapper dwCertMapper;

    @Override
    public int importDWCertData(List<DWCert> dwCertList) {
        return dwCertMapper.importDWCertData(dwCertList);
    }

    @Override
    public boolean selectDWCertNumberCount(String certNumber) {
        return dwCertMapper.selectDWCertNumberCount(certNumber) > 0;
    }
}
